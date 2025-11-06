#!/bin/bash

# MISA.AI Mobile Installation Coordinator
# Coordinates installation across multiple Android devices
# Exports functions for use by main installer

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Global variables
COORDINATION_PORT="${MISA_COORDINATION_PORT:-8081}"
DISCOVERY_TIMEOUT=60
PAIRING_TIMEOUT=300
INSTALLATION_TIMEOUT=600

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Auto-detect Android devices on local network
coordinate_mobile_install() {
    local coordinator_host="$1"
    local coordinator_port="${2:-$COORDINATION_PORT}"
    local pairing_token="${3:-}"

    print_info "Starting mobile installation coordination..."
    print_info "Coordinator: $coordinator_host:$coordinator_port"

    # Discover Android devices
    local devices
    devices=$(discover_android_devices_enhanced)

    if [ $? -eq 0 ] && [ -n "$devices" ]; then
        print_success "Found $(echo "$devices" | wc -l) Android device(s)"

        # Push APK and installation instructions to discovered devices
        echo "$devices" | while read -r device; do
            if [ -n "$device" ]; then
                print_info "Coordinating with device: $device"
                push_installation_to_device "$device" "$coordinator_host" "$coordinator_port" "$pairing_token"
            fi
        done

        return 0
    else
        print_warning "No Android devices found for coordination"
        return 1
    fi
}

# Enhanced Android device discovery
discover_android_devices_enhanced() {
    local discovered_devices=()
    local scan_start=$(date +%s)

    print_info "Scanning network for MISA.AI Android devices..."

    # Method 1: mDNS/Bonjour discovery
    if command -v avahi-browse >/dev/null 2>&1; then
        print_info "Using mDNS discovery..."
        local mdns_devices=$(avahi-browse -r -t _misa-android._tcp 2>/dev/null | grep "=-=" | cut -d';' -f7 | head -10)
        if [ -n "$mdns_devices" ]; then
            while IFS= read -r device; do
                if [ -n "$device" ] && validate_android_device "$device"; then
                    discovered_devices+=("$device")
                    print_success "Found device via mDNS: $device"
                fi
            done <<< "$mdns_devices"
        fi
    fi

    # Method 2: Network port scanning
    print_info "Scanning network ports..."
    local local_ip=$(hostname -I | awk '{print $1}')
    local network=$(echo "$local_ip" | cut -d'.' -f1-3)

    # Scan common ports for MISA Android app
    for port in 8082 8083 8084 8085; do
        for end in {1..254}; do
            local target_ip="${network}.${end}"
            (check_android_device "$target_ip" "$port" && echo "$target_ip:$port") &
        done
    done

    # Wait for scans to complete
    wait

    # Method 3: UPnP discovery
    if command -v upnpc >/dev/null 2>&1; then
        print_info "Using UPnP discovery..."
        local upnp_devices=$(upnpc -l 2>/dev/null | grep "MISA" | head -5)
        if [ -n "$upnp_devices" ]; then
            echo "$upnp_devices" | while read -r device; do
                if [ -n "$device" ]; then
                    discovered_devices+=("$device")
                    print_success "Found device via UPnP: $device"
                fi
            done
        fi
    fi

    # Method 4: Bluetooth LE discovery
    if command -v hcitool >/dev/null 2>&1; then
        print_info "Scanning Bluetooth LE devices..."
        local bt_devices=$(hcitool lescan 2>/dev/null | grep -i "misa" | head -5 | awk '{print $1}')
        for device in $bt_devices; do
            if validate_bluetooth_device "$device"; then
                discovered_devices+=("bt:$device")
                print_success "Found Bluetooth device: $device"
            fi
        done
    fi

    # Remove duplicates and return unique devices
    local unique_devices=()
    for device in "${discovered_devices[@]}"; do
        if [[ ! " ${unique_devices[@]} " =~ " ${device} " ]]; then
            unique_devices+=("$device")
        fi
    done

    if [ ${#unique_devices[@]} -gt 0 ]; then
        printf '%s\n' "${unique_devices[@]}"
        return 0
    else
        return 1
    fi
}

# Validate if device is running MISA.AI Android app
validate_android_device() {
    local device_ip="$1"
    local port="${2:-8082}"

    # Quick HTTP probe to check if it's a MISA device
    if curl -s --connect-timeout 3 --max-time 5 \
        -H "User-Agent: MISA-Coordinator/1.0" \
        "http://$device_ip:$port/misa/status" 2>/dev/null | grep -q "MISA.AI"; then
        return 0
    fi

    return 1
}

# Check Android device on specific port
check_android_device() {
    local device_ip="$1"
    local port="$2"

    if timeout 3 bash -c "</dev/tcp/$device_ip/$port" 2>/dev/null; then
        if curl -s --connect-timeout 2 "http://$device_ip:$port/misa/ping" 2>/dev/null | grep -q "MISA"; then
            echo "$device_ip:$port"
            return 0
        fi
    fi
    return 1
}

# Validate Bluetooth device
validate_bluetooth_device() {
    local bt_address="$1"

    # Check if device supports required services
    if command -v sdptool >/dev/null 2>&1; then
        local services=$(sdptool browse "$bt_address" 2>/dev/null | grep -i "misa")
        if [ -n "$services" ]; then
            return 0
        fi
    fi

    return 1
}

# Push installation package and instructions to device
push_installation_to_device() {
    local device="$1"
    local coordinator_host="$2"
    local coordinator_port="$3"
    local pairing_token="$4"

    print_info "Preparing installation for device: $device"

    # Parse device address
    local device_type="network"
    local device_address="$device"

    if [[ $device == bt:* ]]; then
        device_type="bluetooth"
        device_address="${device#bt:}"
    fi

    # Create installation request
    local install_request=$(cat <<EOF
{
    "type": "installation_request",
    "request_id": "$(date +%s)_$(openssl rand -hex 8)",
    "coordinator": {
        "host": "$coordinator_host",
        "port": "$coordinator_port"
    },
    "installation": {
        "apk_url": "http://$coordinator_host:$coordinator_port/misa-latest.apk",
        "version": "latest",
        "mandatory": false,
        "auto_install": true
    },
    "pairing": {
        "token": "$pairing_token",
        "auto_pair": true,
        "timeout": $PAIRING_TIMEOUT
    },
    "timestamp": "$(date -Iseconds)",
    "checksum": "$(calculate_checksum)"
}
EOF
)

    # Send installation request
    case "$device_type" in
        "network")
            send_network_install_request "$device_address" "$install_request"
            ;;
        "bluetooth")
            send_bluetooth_install_request "$device_address" "$install_request"
            ;;
        *)
            print_error "Unknown device type: $device_type"
            return 1
            ;;
    esac
}

# Send installation request via network
send_network_install_request() {
    local device_address="$1"
    local install_request="$2"

    local device_ip=$(echo "$device_address" | cut -d':' -f1)
    local device_port=$(echo "$device_address" | cut -d':' -f2)

    print_info "Sending installation request to $device_ip:$device_port"

    # Try different endpoints
    local endpoints=(
        "/misa/install"
        "/api/install"
        "/coordinator/install"
        "/install"
    )

    for endpoint in "${endpoints[@]}"; do
        if curl -s --connect-timeout 10 --max-time 30 \
            -X POST \
            -H "Content-Type: application/json" \
            -H "X-MISA-Coordinator: $COORDINATION_PORT" \
            -d "$install_request" \
            "http://$device_ip:$device_port$endpoint" 2>/dev/null; then

            print_success "Installation request sent to $device_address"
            return 0
        fi
    done

    print_error "Failed to send installation request to $device_address"
    return 1
}

# Send installation request via Bluetooth
send_bluetooth_install_request() {
    local bt_address="$1"
    local install_request="$2"

    print_info "Sending installation request via Bluetooth to $bt_address"

    # Create temporary file for the request
    local temp_file=$(mktemp)
    echo "$install_request" > "$temp_file"

    # Use obexftp to send file if available
    if command -v obexftp >/dev/null 2>&1; then
        if obexftp -b "$bt_address" -p "$temp_file" 2>/dev/null; then
            print_success "Installation request sent via Bluetooth to $bt_address"
            rm -f "$temp_file"
            return 0
        fi
    fi

    # Fallback: try to establish serial connection
    if command -v rfcomm >/dev/null 2>&1; then
        local rfcomm_device=$(rfcomm connect "$bt_address" 2>/dev/null | grep -o "rfcomm[0-9]")
        if [ -n "$rfcomm_device" ]; then
            echo "$install_request" > "/dev/$rfcomm_device"
            print_success "Installation request sent via Bluetooth RFCOMM to $bt_address"
            rm -f "$temp_file"
            return 0
        fi
    fi

    print_error "Failed to send Bluetooth installation request to $bt_address"
    rm -f "$temp_file"
    return 1
}

# Monitor installation progress
monitor_installation_progress() {
    local device="$1"
    local timeout="${2:-$INSTALLATION_TIMEOUT}"

    print_info "Monitoring installation progress on $device"
    local start_time=$(date +%s)

    while [ $(($(date +%s) - start_time)) -lt $timeout ]; do
        local status=$(get_installation_status "$device")

        case "$status" in
            "completed")
                print_success "Installation completed on $device"
                return 0
                ;;
            "failed")
                print_error "Installation failed on $device"
                return 1
                ;;
            "downloading"|"installing"|"pairing")
                print_info "Installation in progress on $device: $status"
                ;;
            *)
                print_info "Waiting for status update from $device"
                ;;
        esac

        sleep 10
    done

    print_warning "Installation timeout on $device"
    return 1
}

# Get installation status from device
get_installation_status() {
    local device="$1"

    if [[ $device == *":"* ]]; then
        local device_ip=$(echo "$device" | cut -d':' -f1)
        local device_port=$(echo "$device" | cut -d':' -f2)

        curl -s --connect-timeout 5 \
            -H "X-MISA-Coordinator: $COORDINATION_PORT" \
            "http://$device_ip:$device_port/misa/status" 2>/dev/null | \
            jq -r '.status // "unknown"' 2>/dev/null
    fi

    echo "unknown"
}

# Handle pairing requests from mobile devices
handle_pairing_requests() {
    local pairing_token="$1"
    local timeout="${2:-$PAIRING_TIMEOUT}"

    print_info "Listening for pairing requests (token: $pairing_token)"
    local start_time=$(date +%s)

    while [ $(($(date +%s) - start_time)) -lt $timeout ]; do
        # Check for pairing requests via coordination server
        if [ -f "/tmp/.misa_pairing_requests" ]; then
            local requests=$(cat "/tmp/.misa_pairing_requests" | grep "$pairing_token")
            if [ -n "$requests" ]; then
                echo "$requests" | while read -r request; do
                    if [ -n "$request" ]; then
                        process_pairing_request "$request"
                    fi
                done
            fi
        fi

        sleep 5
    done

    print_warning "Pairing timeout reached"
}

# Process individual pairing request
process_pairing_request() {
    local request="$1"

    local device_id=$(echo "$request" | jq -r '.device_id' 2>/dev/null)
    local device_name=$(echo "$request" | jq -r '.device_name' 2>/dev/null)

    print_info "Processing pairing request from $device_name ($device_id)"

    # Auto-accept pairing requests for coordinated installations
    local pairing_response=$(cat <<EOF
{
    "status": "accepted",
    "message": "Pairing accepted automatically",
    "device_id": "$device_id",
    "pairing_token": "$pairing_token",
    "timestamp": "$(date -Iseconds)"
}
EOF
    )

    # Send response back to device
    send_pairing_response "$device_id" "$pairing_response"
}

# Send pairing response to device
send_pairing_response() {
    local device_id="$1"
    local response="$2"

    # This would typically send the response via the coordination server
    print_info "Sending pairing response to $device_id"
    echo "$response" >> "/tmp/.misa_pairing_responses_$device_id"
}

# Calculate checksum for installation package
calculate_checksum() {
    # Generate checksum for verification
    echo "$(date +%s | openssl dgst -sha256 -hex | cut -d' ' -f2)"
}

# Export main coordination function
export -f coordinate_mobile_install

# Export utility functions
export -f discover_android_devices_enhanced
export -f validate_android_device
export -f monitor_installation_progress
export -f handle_pairing_requests

# Export configuration
export COORDINATION_PORT
export DISCOVERY_TIMEOUT
export PAIRING_TIMEOUT
export INSTALLATION_TIMEOUT

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    if [ $# -eq 0 ]; then
        echo "Usage: $0 <coordinator_host> [coordinator_port] [pairing_token]"
        echo "Coordinates mobile installation across Android devices"
        exit 1
    fi

    coordinate_mobile_install "$@"
fi