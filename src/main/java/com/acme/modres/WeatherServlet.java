package com.acme.modres;

import com.acme.modres.db.ModResortsCustomerInformation;
import com.acme.modres.mbean.AppInfo;
import com.acme.modres.weather.WeatherService;
import com.acme.modres.weather.WeatherServiceClient;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * WeatherServlet is a thin HTTP adapter that delegates all weather-retrieval
 * logic to the independently deployable {@link WeatherService} microservice.
 *
 * <p><b>cz-java-0082 – Individual Components (remediation)</b>: The monolithic
 * weather logic that was previously embedded directly in this servlet has been
 * extracted into {@link WeatherServiceClient}, which implements the
 * {@link WeatherService} interface.  This decomposition enables:
 * <ul>
 *   <li>Independent deployment of the weather concern as its own Kubernetes
 *       Deployment on Amazon EKS</li>
 *   <li>A dedicated Kubernetes Service and ConfigMap per component</li>
 *   <li>Loose coupling via the {@code WEATHER_SERVICE_URL} environment variable
 *       (Kubernetes DNS-based service discovery)</li>
 * </ul>
 *
 * <p>This servlet now acts solely as the HTTP entry-point, forwarding requests
 * to {@link WeatherService} and writing the JSON response back to the client.
 */
@WebServlet({ "/resorts/weather" })
public class WeatherServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Environment variable key for the weather API key. */
    private static final String WEATHER_API_KEY = "WEATHER_API_KEY";

    private static final Logger logger = Logger.getLogger(WeatherServlet.class.getName());

    @Inject
    private ModResortsCustomerInformation customerInfo;

    /**
     * Independently deployable weather microservice client.
     * Configured via {@code WEATHER_SERVICE_URL} environment variable so that
     * the service endpoint can be managed through a Kubernetes ConfigMap without
     * redeploying this servlet.
     */
    private WeatherService weatherService;

    MBeanServer server;
    ObjectName weatherON;
    ObjectInstance mbean;

    @Override
    public void init() {
        server = ManagementFactory.getPlatformMBeanServer();
        try {
            weatherON = new ObjectName("com.acme.modres.mbean:name=appInfo");
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        try {
            if (weatherON != null) {
                mbean = server.registerMBean(new AppInfo(), weatherON);
            }
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            e.printStackTrace();
        }

        // Instantiate the independently deployable weather service client.
        // The client resolves the external microservice URL from the
        // WEATHER_SERVICE_URL environment variable at runtime, enabling
        // Kubernetes-native service discovery without any code changes.
        weatherService = new WeatherServiceClient();
    }

    @Override
    public void destroy() {
        if (mbean != null) {
            try {
                server.unregisterMBean(weatherON);
            } catch (MBeanRegistrationException | InstanceNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {

        String methodName = "doGet";
        logger.entering(WeatherServlet.class.getName(), methodName);

        try {
            MBeanInfo weatherConfig = server.getMBeanInfo(weatherON);
        } catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
            e.printStackTrace();
        }

        String city = request.getParameter("selectedCity");
        logger.log(Level.FINE, "requested city is " + city);

        String weatherAPIKey = System.getenv(WEATHER_API_KEY);
        String mockedKey = mockKey(weatherAPIKey);
        logger.log(Level.FINE, "weatherAPIKey is " + mockedKey);

        // Delegate to the independently deployable WeatherService microservice.
        // All weather-retrieval logic lives in WeatherServiceClient (cz-java-0082).
        String weatherJson = weatherService.getWeatherData(city, weatherAPIKey);

        if (weatherJson != null) {
            ServletOutputStream out = null;
            try {
                response.setContentType("application/json");
                out = response.getOutputStream();
                out.print(weatherJson);
                logger.log(Level.FINE, "Weather response written for city: " + city);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    /**
     * Delegates POST requests to {@link #doGet(HttpServletRequest, HttpServletResponse)}.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private static String mockKey(String toBeMocked) {
        if (toBeMocked == null) {
            return null;
        }
        String lastToKeep = toBeMocked.substring(toBeMocked.length() - 3);
        return "*********" + lastToKeep;
    }
}
