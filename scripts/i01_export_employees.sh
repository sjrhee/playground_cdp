#!/bin/bash
set -e

# Define variables
CONTAINER_NAME="demo_mysql"
MYSQL_USER="root"
MYSQL_PASSWORD="rootpassword"
MYSQL_DATABASE="mysql_employees"
# Path defined in the SQL file (matches secure-file-priv)
EXPORT_PATH_IN_CONTAINER="/docker-entrypoint-initdb.d/employee_export.csv"
LOCAL_OUTPUT_FILE="./employee_export.csv"
SQL_SCRIPT="i01_export_employees.sql"

# Get absolute path to the SQL script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE_PATH="$SCRIPT_DIR/$SQL_SCRIPT"

echo "Step 1: Cleaning up existing export file in container..."
docker exec $CONTAINER_NAME rm -f $EXPORT_PATH_IN_CONTAINER

echo "Step 2: Executing Export SQL..."
if [ ! -f "$SQL_FILE_PATH" ]; then
    echo "Error: SQL file not found at $SQL_FILE_PATH"
    exit 1
fi

# Use root user for FILE privilege
cat "$SQL_FILE_PATH" | docker exec -i $CONTAINER_NAME mysql -u$MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DATABASE

echo "Step 3: Copying exported file to local host..."
# Since /docker-entrypoint-initdb.d is mounted to host, we can just copy/move it from the mount point usually,
# but docker cp is safer if we don't assume the mount location relative execution.
docker cp $CONTAINER_NAME:$EXPORT_PATH_IN_CONTAINER $LOCAL_OUTPUT_FILE

echo "Success! Exported to $LOCAL_OUTPUT_FILE"
