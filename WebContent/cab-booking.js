// cab-booking.js - Uber cab booking functionality

var currentBookingId = null;
var pollingInterval = null;

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    loadReservationId();
    initializeDatePicker();
    setupFormSubmit();
});

function loadReservationId() {
    var reservationId = sessionStorage.getItem('currentReservationId');
    if (reservationId) {
        document.getElementById('reservationId').value = reservationId;
    } else {
        showError('No reservation found. Please make a reservation first.');
        document.getElementById('bookButton').disabled = true;
    }
}

function initializeDatePicker() {
    var picker = new Pikaday({
        field: document.getElementById('pickupDate'),
        firstDay: 1,
        minDate: new Date(),
        maxDate: new Date(new Date().setFullYear(new Date().getFullYear() + 1)),
        format: 'MM/DD/YYYY',
        yearRange: [new Date().getFullYear(), new Date().getFullYear() + 1]
    });
}

function setupFormSubmit() {
    var form = document.getElementById('cabBookingForm');
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        bookCab();
    });
}

function bookCab() {
    clearMessages();

    var reservationId = document.getElementById('reservationId').value;
    var destination = document.getElementById('destination').value;
    var pickupDate = document.getElementById('pickupDate').value;
    var pickupTime = document.getElementById('pickupTime').value;
    var rideType = document.getElementById('rideType').value;

    if (!reservationId || !destination) {
        showError('Please fill in all required fields');
        return;
    }

    var pickupTimeFormatted = null;
    if (pickupDate && pickupTime) {
        pickupTimeFormatted = pickupDate + ' ' + pickupTime;
    }

    var requestData = {
        reservationId: reservationId,
        destination: destination,
        pickupTime: pickupTimeFormatted,
        rideType: rideType
    };

    var bookButton = document.getElementById('bookButton');
    bookButton.disabled = true;
    bookButton.textContent = 'Requesting...';

    var getUrl = window.location;
    var baseUrl = getUrl.protocol + "//" + getUrl.hostname + ":" + getUrl.port;

    var request = new XMLHttpRequest();
    request.open('POST', baseUrl + '/resorts/cabs', true);
    request.setRequestHeader('Content-Type', 'application/json');

    request.onload = function() {
        bookButton.disabled = false;
        bookButton.textContent = 'Request Uber';

        if (request.status >= 200 && request.status < 300) {
            try {
                var response = JSON.parse(request.responseText);
                handleBookingSuccess(response);
            } catch (e) {
                showError('Error processing response');
            }
        } else {
            try {
                var errorResponse = JSON.parse(request.responseText);
                showError(errorResponse.error || 'Failed to book ride');
            } catch (e) {
                showError('Failed to book ride. Please try again.');
            }
        }
    };

    request.onerror = function() {
        bookButton.disabled = false;
        bookButton.textContent = 'Request Uber';
        showError('Network error. Please check your connection and try again.');
    };

    request.send(JSON.stringify(requestData));
}

function handleBookingSuccess(response) {
    showSuccess('Ride requested successfully! Booking ID: ' + response.cabBookingId);
    currentBookingId = response.cabBookingId;

    displayRideStatus(response);

    document.getElementById('cabBookingForm').reset();
    loadReservationId();

    startStatusPolling();
}

function displayRideStatus(response) {
    var statusDiv = document.getElementById('rideStatus');
    statusDiv.style.display = 'block';

    document.getElementById('statusBookingId').textContent = response.cabBookingId || 'N/A';
    document.getElementById('statusStatus').textContent = response.status || 'REQUESTED';
    document.getElementById('statusEta').textContent = response.estimatedArrival || 'Calculating...';
    document.getElementById('statusDriver').textContent = response.driverName || 'Not assigned yet';
    document.getElementById('statusVehicle').textContent = response.vehicleDetails || 'Not assigned yet';
}

function startStatusPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
    }

    pollingInterval = setInterval(function() {
        if (currentBookingId) {
            refreshRideStatus(currentBookingId);
        }
    }, 30000);
}

function refreshRideStatus(bookingId) {
    var getUrl = window.location;
    var baseUrl = getUrl.protocol + "//" + getUrl.hostname + ":" + getUrl.port;

    var request = new XMLHttpRequest();
    request.open('GET', baseUrl + '/resorts/cabs/' + bookingId + '?refresh=true', true);

    request.onload = function() {
        if (request.status >= 200 && request.status < 300) {
            try {
                var response = JSON.parse(request.responseText);
                displayRideStatus(response);

                if (response.status === 'COMPLETED' || response.status === 'CANCELLED') {
                    stopStatusPolling();
                }
            } catch (e) {
                console.error('Error parsing status response', e);
            }
        }
    };

    request.onerror = function() {
        console.error('Error fetching ride status');
    };

    request.send();
}

function stopStatusPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }
}

function showError(message) {
    var errorDiv = document.getElementById('errorMessage');
    errorDiv.textContent = message;
    errorDiv.style.display = 'block';

    setTimeout(function() {
        errorDiv.style.display = 'none';
    }, 5000);
}

function showSuccess(message) {
    var successDiv = document.getElementById('successMessage');
    successDiv.textContent = message;
    successDiv.style.display = 'block';

    setTimeout(function() {
        successDiv.style.display = 'none';
    }, 5000);
}

function clearMessages() {
    document.getElementById('errorMessage').style.display = 'none';
    document.getElementById('successMessage').style.display = 'none';
}
