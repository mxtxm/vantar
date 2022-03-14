DROP DATABASE IF EXISTS news_parser;
DROP ROLE IF EXISTS newser;

CREATE USER newser WITH PASSWORD 'newser';

CREATE DATABASE news_parser ENCODING 'UTF-8';
GRANT ALL PRIVILEGES ON database news_parser TO newser;
