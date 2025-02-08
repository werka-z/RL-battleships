import socket
import random

class BattleshipAIServer:
    def __init__(self, port=12345):
        self.port = port
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.hits = set()
        self.misses = set()
        self.last_hit = None

        try:
            self.server_socket.bind(('localhost', self.port))
            self.server_socket.listen(1)
            print(f"AI waiting for connection on port {self.port}")

            self.socket, addr = self.server_socket.accept()
            print(f"Connected to game at {addr}")
        except Exception as e:
            print(f"Error initializing server: {e}")
            raise

    def get_next_target(self):
        # If we have a hit, try adjacent squares
        if self.last_hit:
            row, col = self.last_hit
            for dr, dc in [(0,1), (1,0), (0,-1), (-1,0)]:
                new_row, new_col = row + dr, col + dc
                if (0 <= new_row < 10 and 0 <= new_col < 10 and
                        (new_row, new_col) not in self.hits and
                        (new_row, new_col) not in self.misses):
                    return f"{chr(ord('A') + new_col)}{new_row + 1}"

        # Otherwise, try a random untried position
        available = [(r,c) for r in range(10) for c in range(10)
                     if (r,c) not in self.hits and (r,c) not in self.misses]
        if not available:
            return None

        row, col = random.choice(available)
        return f"{chr(ord('A') + col)}{row + 1}"

    def update_state(self, result, coords):
        if not coords:
            return

        # Convert coordinates to row/col
        col = ord(coords[0].upper()) - ord('A')
        row = int(coords[1:]) - 1

        if result == "hit":
            self.hits.add((row, col))
            self.last_hit = (row, col)
        elif result == "miss":
            self.misses.add((row, col))
            self.last_hit = None
        elif result in ["hit and sunk", "last ship sunk"]:
            self.hits.add((row, col))
            self.last_hit = None

    def run(self):
        try:
            while True:
                data = self.socket.recv(1024).decode('utf-8').strip()
                if not data:
                    break

                print(f"Received: {data}")

                parts = data.split(';')
                result = parts[0]
                coords = parts[1] if len(parts) > 1 else None

                self.update_state(result, coords)

                if result == "last ship sunk":
                    print("Game over!")
                    break

                next_target = self.get_next_target()
                if not next_target:
                    print("No valid targets remaining!")
                    break

                self.socket.sendall(f"{next_target}\n".encode('utf-8'))
                print(f"Sent target: {next_target}")

        finally:
            self.socket.close()
            self.server_socket.close()

if __name__ == "__main__":
    server = BattleshipAIServer()
    server.run()