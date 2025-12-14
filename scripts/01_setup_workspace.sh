#!/bin/bash
set -e

echo "Step 1: Build Java Application"

# Script is in playgroud_cdp/scripts, so root is ..
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "Project Root: $BASE_DIR"

# Check for pom.xml in project root
if [ ! -f "$BASE_DIR/pom.xml" ]; then
    echo "Error: pom.xml not found in $BASE_DIR"
    exit 1
fi

echo "Building project in $BASE_DIR..."
cd "$BASE_DIR"
mvn clean package -DskipTests

# Verify build output
if [ -d "target/ROOT" ]; then
    echo "Build successful. Exploded WAR found in target/ROOT."
else
    # If war is packaged as ROOT.war, unzip it
    if [ -f "target/ROOT.war" ]; then
         echo "Unzipping ROOT.war..."
         mkdir -p target/ROOT
         unzip -o target/ROOT.war -d target/ROOT
    elif [ -f "target/*.war" ]; then 
          # Handle named war if not ROOT.war
          WAR_FILE=$(ls target/*.war | head -n 1)
          echo "Unzipping $WAR_FILE..."
          mkdir -p target/ROOT
          unzip -o "$WAR_FILE" -d target/ROOT
    else
        echo "Error: Build artifact not found."
        exit 1
    fi
fi

echo "Copying build artifacts to deployment directory..."
# Deployment directory is tomcat_mysql_docker/target/ROOT
DEPLOY_DIR="$BASE_DIR/tomcat_mysql_docker/target/ROOT"
mkdir -p "$DEPLOY_DIR"
cp -r "$BASE_DIR/target/ROOT/"* "$DEPLOY_DIR/"

echo "Build and setup complete."
