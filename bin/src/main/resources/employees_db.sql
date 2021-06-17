CREATE TABLE IF NOT EXISTS employees
(
    id VARCHAR(10) PRIMARY KEY,
    login varchar(60) NOT NULL UNIQUE,
    name VARCHAR(60) NOT NULL,
    salary REAL NOT NULL CHECK(salary >= 0),
    startDate DATE NOT NULL
);