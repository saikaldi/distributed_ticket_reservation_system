-- Enable UUID extension for primary keys
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- venues (concert halls, stadiums, theaters)
CREATE TABLE venues (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        name VARCHAR(255) NOT NULL,
                        address VARCHAR(255) NOT NULL,
                        total_capacity INT NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- events (concerts, shows, sports games)
CREATE TABLE events (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        start_time TIMESTAMP WITH TIME ZONE NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- seats (physical seats in a venue)
CREATE TABLE seats (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
                       row_number INT NOT NULL,
                       seat_number INT NOT NULL,
                       category VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
                       price DECIMAL(10, 2) NOT NULL,
                       CONSTRAINT unique_venue_row_seat UNIQUE (venue_id, row_number, seat_number)
);

-- users (customers and administrators)
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- reservations (temporary seat locks)
CREATE TABLE reservations (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                              seat_id UUID NOT NULL REFERENCES seats(id) ON DELETE CASCADE,
                              status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                              expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- tickets (confirmed ticket purchases)
CREATE TABLE tickets (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         reservation_id UUID NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
                         user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         seat_id UUID NOT NULL REFERENCES seats(id) ON DELETE CASCADE,
                         ticket_code VARCHAR(100) NOT NULL UNIQUE,
                         status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                         issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);