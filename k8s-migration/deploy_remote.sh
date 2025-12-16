#!/bin/bash
set -e

# Configuration
REMOTE_USER="ubuntu"
MASTER_NODE="192.168.100.11"
# List all nodes here
TARGET_NODES=("192.168.100.11" "192.168.100.12")
REMOTE_DIR="~/k8s-migration"
IMAGE_TAR="k8s-migration/playground-web.tar"
IMAGE_NAME="playground-web:latest"

# Check if tar exists
if [ ! -f "$IMAGE_TAR" ]; then
    echo "Error: $IMAGE_TAR not found. Please run build_and_save.sh first."
    exit 1
fi

echo ">>> Starting Remote Deployment..."

# 1. Distribute Image to All Nodes
for NODE in "${TARGET_NODES[@]}"; do
    echo ">>> processing node: $NODE"
    
    # Create Directory
    ssh $REMOTE_USER@$NODE "mkdir -p $REMOTE_DIR"
    
    # Transfer Tarball (Only if not already there? standard scp overwrites)
    echo "    [SCP] Transferring image..."
    scp $IMAGE_TAR $REMOTE_USER@$NODE:$REMOTE_DIR/
    
    # Import Image
    echo "    [CTR] Importing image..."
    ssh $REMOTE_USER@$NODE "sudo ctr -n k8s.io images import $REMOTE_DIR/playground-web.tar"
    
    # Cleanup Tarball on remote to save space? (Optional, maybe keep for now)
    # ssh $REMOTE_USER@$NODE "rm $REMOTE_DIR/playground-web.tar"
done

# 2. Transfer Manifests to Master
echo ">>> Transferring manifests to Master ($MASTER_NODE)..."
scp k8s-migration/mysql-external.yaml k8s-migration/web-deployment.yaml $REMOTE_USER@$MASTER_NODE:$REMOTE_DIR/

# 3. Apply Kubernetes Manifests (on Master)
echo ">>> Applying Kubernetes manifests on Master..."
ssh $REMOTE_USER@$MASTER_NODE << EOF
    # Apply External MySQL Service
    kubectl apply -f $REMOTE_DIR/mysql-external.yaml
    
    # Apply Web Deployment
    kubectl apply -f $REMOTE_DIR/web-deployment.yaml
EOF

echo ">>> Deployment Complete!"
echo "Check pods status on master: ssh $REMOTE_USER@$MASTER_NODE 'kubectl get pods'"
