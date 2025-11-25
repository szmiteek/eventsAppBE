SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS eventapp
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE eventapp;


CREATE TABLE IF NOT EXISTS employee (
                                        id INT AUTO_INCREMENT PRIMARY KEY,
                                        first_name VARCHAR(255),
    last_name VARCHAR(255),
    hourly_rate DECIMAL(10,2)
    );


CREATE TABLE IF NOT EXISTS offer (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     created_date DATE,
                                     personal_data VARCHAR(255),
    venue VARCHAR(255),
    event_date DATE,
    email VARCHAR(255),
    phone VARCHAR(255),
    budget INT,
    guests INT,
    price DECIMAL(10,2),
    comment VARCHAR(255),
    status VARCHAR(50)
    );


CREATE TABLE IF NOT EXISTS event (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     client_personal_data VARCHAR(255),
    venue VARCHAR(255),
    date DATE,
    email VARCHAR(255),
    phone VARCHAR(255),
    budget INT,
    guests INT,
    price DECIMAL(10,2),
    comment VARCHAR(255),
    offer_id INT,
    CONSTRAINT fk_event_offer FOREIGN KEY (offer_id) REFERENCES offer(id)
    );


CREATE TABLE IF NOT EXISTS event_work (
                                          id INT AUTO_INCREMENT PRIMARY KEY,
                                          event_id INT NOT NULL,
                                          employee_id INT NOT NULL,
                                          hours_worked DOUBLE,
                                          CONSTRAINT fk_event_work_event FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_work_employee FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    CONSTRAINT unique_event_employee UNIQUE (event_id, employee_id)
    );

INSERT INTO offer (created_date, personal_data, venue, event_date, email, phone, budget, guests, price, comment, status)
VALUES
    (CURDATE(), 'Jan Kowalski', 'Sala A', CURDATE() + INTERVAL 10 DAY, 'jan@example.com', '123456789', 1000, 50, 900, 'Komentarz 1', 'NOT_SENT'),
    (CURDATE(), 'Anna Nowak', 'Sala B', CURDATE() + INTERVAL 12 DAY, 'anna@example.com', '987654321', 1200, 60, 1100, 'Komentarz 2', 'SENT'),
    (CURDATE(), 'Piotr Wiśniewski', 'Sala C', CURDATE() + INTERVAL 15 DAY, 'piotr@example.com', '555444333', 1500, 70, 1400, 'Komentarz 3', 'SIGNED'),
    (CURDATE(), 'Kasia Zielińska', 'Sala D', CURDATE() + INTERVAL 8 DAY, 'kasia@example.com', '222333444', 800, 40, 750, 'Komentarz 4', 'NOT_SENT'),
    (CURDATE(), 'Łukasz Grabowski', 'Sala M', CURDATE() + INTERVAL 16 DAY, 'lukasz@example.com', '888999000', 1700, 90, 1600, 'Komentarz 5', 'SIGNED');