create database "mini_dish_db";

create user "mini_dish_db_manager" with password '123456';

GRANT CONNECT ON DATABASE product_management_db TO product_manager_user;

GRANT CREATE ON SCHEMA public TO product_manager_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO product_manager_user;