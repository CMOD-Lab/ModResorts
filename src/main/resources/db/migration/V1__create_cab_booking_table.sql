-- Flyway migration script for CAB_BOOKING table
-- Version: V1
-- Description: Create cab booking table with foreign key to reservation

CREATE TABLE CAB_BOOKING (
    id VARCHAR(36) PRIMARY KEY,
    reservation_id VARCHAR(36) NOT NULL,
    uber_ride_id VARCHAR(255),
    pickup_address VARCHAR(500),
    dropoff_address VARCHAR(500) NOT NULL,
    scheduled_datetime TIMESTAMP,
    booking_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    driver_name VARCHAR(255),
    vehicle_details VARCHAR(255),
    estimated_arrival VARCHAR(50)
);

-- Add index on reservation_id for fast lookup
CREATE INDEX idx_reservation_id ON CAB_BOOKING(reservation_id);

-- Note: Foreign key constraint requires RESERVATION table to exist
-- Uncomment the following line when RESERVATION table is created:
-- ALTER TABLE CAB_BOOKING ADD CONSTRAINT fk_reservation
--     FOREIGN KEY (reservation_id) REFERENCES RESERVATION(id) ON DELETE CASCADE;
