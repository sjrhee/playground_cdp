#!/bin/bash
set -e

# 스크립트가 있는 디렉토리의 상위 디렉토리(프로젝트 루트)로 이동
cd "$(dirname "$0")/.."

echo "Starting Build Process..."

# 1. Maven Build
echo ">>> Building Maven Project..."
mvn clean package -DskipTests

# 2. Docker Build
echo ">>> Building Docker Image..."
docker build -t playground-web:latest -f k8s-migration/Dockerfile .

# 3. Save Docker Image to Tar
echo ">>> Saving Docker Image to Tar file..."
OUTPUT_TAR="k8s-migration/playground-web.tar"
docker save -o $OUTPUT_TAR playground-web:latest

echo "Build Complete!"
echo "Docker image saved to: $OUTPUT_TAR"
echo ""
echo "Next Steps:"
echo "1. Transfer the tar file to your K8s node:"
echo "   scp $OUTPUT_TAR user@k8s-node:/path/to/destination"
echo ""
echo "2. Load the image on the K8s node:"
echo "   docker load -i playground-web.tar"
echo "   # OR for containerd/crictl environments:"
echo "   # ctr -n k8s.io images import playground-web.tar"
echo ""
echo "3. Apply Kubernetes manifests:"
echo "   kubectl apply -f mysql-external.yaml"
echo "   kubectl apply -f web-deployment.yaml"
