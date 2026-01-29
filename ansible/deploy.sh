#!/bin/bash
# Quick deployment script for AI Receipt Backend
# Usage: ./deploy.sh [mysql|java-app|all] [password]

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
DEPLOYMENT_TYPE="${1:-mysql}"
VAULT_PASSWORD="${2:-}"

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Validate inputs
if [[ ! "$DEPLOYMENT_TYPE" =~ ^mysql$ ]]; then
    print_error "Invalid deployment type. Use: mysql"
fi

# Check if ansible is installed
if ! command -v ansible-playbook &> /dev/null; then
    print_error "Ansible is not installed. Please install it first."
fi

# Prompt for vault password if not provided
if [ -z "$VAULT_PASSWORD" ]; then
    read -sp "Enter Ansible Vault password: " VAULT_PASSWORD
    echo
fi

# Save vault password to temporary file
VAULT_FILE=$(mktemp)
echo "$VAULT_PASSWORD" > "$VAULT_FILE"
trap "rm -f $VAULT_FILE" EXIT

print_status "Starting deployment of type: $DEPLOYMENT_TYPE"

# Build ansible-playbook command
CMD="ansible-playbook deploy.yml \
    -i hosts.ini \
    --vault-password-file=$VAULT_FILE"

case "$DEPLOYMENT_TYPE" in
    mysql)
        print_status "Deploying MySQL..."
        ;;
esac

# Add sudo prompt
CMD="$CMD --ask-become-pass"

print_status "Executing: $CMD"
eval "$CMD"

print_status "Deployment completed successfully!"
print_status "Verifying application..."

# Wait a moment for services to start
sleep 5

# Verify services
if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
    print_status "Application is running successfully!"
else
    print_warning "Could not reach application at http://localhost:8080/api/health"
fi
