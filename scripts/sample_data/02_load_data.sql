USE mysql_employees;
SET GLOBAL local_infile=1;
LOAD DATA INFILE '/docker-entrypoint-initdb.d/employee.csv' INTO TABLE employee FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n' IGNORE 1 LINES;
LOAD DATA INFILE '/docker-entrypoint-initdb.d/salary.csv' INTO TABLE salary FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n' IGNORE 1 LINES;
