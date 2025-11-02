#!/usr/bin/env python3
"""
Mock Data Generator for Real-Time Analytics Platform
Generates random analytics events and publishes them to Kafka
"""

import json
import random
import time
from datetime import datetime, timezone
from kafka import KafkaProducer
from kafka.errors import KafkaError

# ============================================
# Configuration Variables
# ============================================

# Number of unique users to generate
NUM_UNIQUE_USERS = 20

# Average number of sessions per user
AVG_SESSIONS_PER_USER = 3

# Predefined page URLs
PAGE_URLS = [
    "/home",
    "/products/electronics",
    "/products/books",
    "/products/clothing",
    "/cart",
    "/checkout",
    "/profile",
    "/search",
    "/products/detail/123",
    "/products/detail/456",
    "/login",
    "/register"
]

# Predefined event types
EVENT_TYPES = [
    "page_view",
    "click",
    "scroll",
    "form_submit",
    "button_click",
    "link_click",
    "video_play",
    "image_view"
]

# Kafka Configuration
# Note: Use localhost:9092 for external access (as per docker-compose.yml)
KAFKA_BOOTSTRAP_SERVERS = ['localhost:9092']
KAFKA_TOPIC = 'analytics-events'

# Event generation rate (events per second)
EVENTS_PER_SECOND = 100

# ============================================
# Helper Functions
# ============================================

def generate_user_id() -> str:
    """Generate a random user ID: usr_XXX (3 digits)"""
    return f"usr_{random.randint(100, 999):03d}"

def generate_session_id() -> str:
    """Generate a random session ID: sess_XXX (3 digits)"""
    return f"sess_{random.randint(100, 999):03d}"

def generate_timestamp() -> str:
    """Generate current timestamp in ISO-8601 format"""
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

def generate_event(user_id: str, session_id: str) -> dict:
    """Generate a random analytics event"""
    return {
        "timestamp": generate_timestamp(),
        "userId": user_id,
        "eventType": random.choice(EVENT_TYPES),
        "pageUrl": random.choice(PAGE_URLS),
        "sessionId": session_id
    }

# ============================================
# Main Function
# ============================================

def main():
    """Generate and publish mock events to Kafka"""
    
    print("Starting Mock Data Generator...")
    print(f"Configuration:")
    print(f"  - Unique Users: {NUM_UNIQUE_USERS}")
    print(f"  - Avg Sessions per User: {AVG_SESSIONS_PER_USER}")
    print(f"  - Events per Second: {EVENTS_PER_SECOND}")
    print(f"  - Kafka Topic: {KAFKA_TOPIC}")
    print(f"  - Kafka Bootstrap: {KAFKA_BOOTSTRAP_SERVERS[0]}\n")
    
    # Generate unique user pool and sessions
    users = []
    used_user_ids = set()
    while len(users) < NUM_UNIQUE_USERS:
        user_id = generate_user_id()
        if user_id not in used_user_ids:
            users.append(user_id)
            used_user_ids.add(user_id)
    
    user_sessions = {}
    for user_id in users:
        num_sessions = max(1, int(random.gauss(AVG_SESSIONS_PER_USER, 1)))
        user_sessions[user_id] = [generate_session_id() for _ in range(num_sessions)]
    
    print(f"Generated {len(users)} users with total {sum(len(s) for s in user_sessions.values())} sessions\n")
    
    # Initialize Kafka producer
    try:
        producer = KafkaProducer(
            bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
            value_serializer=lambda v: json.dumps(v).encode('utf-8'),
            key_serializer=lambda k: k.encode('utf-8') if k else None
        )
        print(f"Connected to Kafka at {KAFKA_BOOTSTRAP_SERVERS[0]}\n")
    except KafkaError as e:
        print(f"Error connecting to Kafka: {e}")
        print("Make sure Kafka is running on localhost:9092")
        return
    
    # Calculate delay between events
    delay = 1.0 / EVENTS_PER_SECOND if EVENTS_PER_SECOND > 0 else 0
    
    print("Publishing events... (Press Ctrl+C to stop)\n")
    
    try:
        event_count = 0
        while True:
            # Randomly select a user and their session
            user_id = random.choice(users)
            session_id = random.choice(user_sessions[user_id])
            
            # Generate event
            event = generate_event(user_id, session_id)
            
            # Publish to Kafka
            future = producer.send(KAFKA_TOPIC, value=event, key=user_id)
            
            # Optional: wait for confirmation (commented for performance)
            # future.get(timeout=10)
            
            event_count += 1
            if event_count % 100 == 0:
                print(f"Published {event_count} events...")
            
            # Wait before next event
            if delay > 0:
                time.sleep(delay)
                
    except KeyboardInterrupt:
        print(f"\n\nStopping generator...")
        print(f"Total events published: {event_count}")
    except KafkaError as e:
        print(f"\nError publishing to Kafka: {e}")
    finally:
        producer.close()
        print("Producer closed. Goodbye!")

if __name__ == "__main__":
    main()

