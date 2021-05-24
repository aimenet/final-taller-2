ip addr add 127.0.0.2/8 dev lo label lo:2
ip addr add 127.0.0.3/8 dev lo label lo:3
ip addr add 127.0.0.4/8 dev lo label lo:4
ip addr add 127.0.0.5/8 dev lo label lo:5

ip addr add 127.0.0.6/8 dev lo label lo:6
ip addr add 127.0.0.7/8 dev lo label lo:7
ip addr add 127.0.0.8/8 dev lo label lo:8
ip addr add 127.0.0.9/8 dev lo label lo:9
ip addr add 127.0.0.10/8 dev lo label lo:10

ip address show | head -n 30
