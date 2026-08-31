-- Run once as a PostgreSQL administrator on the shared Coolify PostgreSQL instance.
-- Replace the password before executing this file.

CREATE ROLE developer_analytics
    LOGIN
    PASSWORD 'CHANGE_ME';

CREATE DATABASE developer_analytics
    OWNER developer_analytics;
