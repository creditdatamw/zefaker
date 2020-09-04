
DROP TABLE businesses;
DROP TABLE users;
DROP TABLE businesses_services;

CREATE TABLE businesses (
    id int not null primary key,
    business_name varchar(255) not null,
    address varchar(255),
    registration_date date,
    email text,
    created_at timestamp,
    modified_at timestamp,
    deleted_at timestamp
);

CREATE TABLE users (
    id int not null primary key,
    business_id int,
    username varchar(255),
    subscription_type varchar(50),
    last_login_at timestamp,
    created_at timestamp,
    modified_at timestamp,
    deleted_at timestamp

);

CREATE TABLE businesses_services (
    id int not null primary key,
    business_id int,
    service_name varchar(255),
    created_at timestamp,
    modified_at timestamp,
    deleted_at timestamp
);
