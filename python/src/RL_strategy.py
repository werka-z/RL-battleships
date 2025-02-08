import random

import numpy as np
from enum import Enum
import pickle
from typing import Tuple

class CellState(Enum):
    UNKNOWN = 0
    MISS = 1
    HIT = 2
    SUNK = 3

class BattleshipAI:
    def __init__(self, board_size=10, learning_rate=0.1):
        self.board_size = board_size
        self.learning_rate = learning_rate
        self.probability_map = np.ones((board_size, board_size)) / (board_size * board_size)
        self.known_board = np.full((board_size, board_size), CellState.UNKNOWN)
        self.remaining_ships = [5, 4, 3, 3, 2]
        self.success_patterns = self._load_patterns()
        self.current_game_moves = []

    def convert_to_coords(self, move: str) -> Tuple[int, int]:
        col = ord(move[0].upper()) - ord('A')
        row = int(move[1:]) - 1
        return row, col

    def convert_to_move(self, x: int, y: int) -> str:
        """Convert coordinates to 'A1' format"""
        col = chr(y + ord('A'))
        row = str(x + 1)
        return f"{col}{row}"

    def update_model(self, move: str, feedback: str):
        """Update the model with move result"""
        x, y = self.convert_to_coords(move)
        self.current_game_moves.append((move, feedback))

        if feedback == "miss":
            self.known_board[x,y] = CellState.MISS
            self.probability_map[x,y] = 0
        elif "hit" in feedback:
            self.known_board[x,y] = CellState.HIT
            if "sunk" in feedback:
                self._handle_sunk_ship(x, y)
                if "last" in feedback:
                    self._learn_from_game()

        self._update_probabilities()

    def choose_move(self) -> str:
        """Choose next move based on probability map and learned patterns"""
        # Combine probability map with learned patterns
        combined_weights = self.probability_map.copy()
        for pattern, success_rate in self.success_patterns.items():
            x, y = self.convert_to_coords(pattern)
            if self.known_board[x,y] == CellState.UNKNOWN:
                combined_weights[x,y] *= (1 + success_rate * self.learning_rate)

        # Normalize and choose highest probability
        valid_moves = (self.known_board == CellState.UNKNOWN)
        combined_weights[~valid_moves] = 0

        if combined_weights.sum() > 0:
            combined_weights /= combined_weights.sum()
            x, y = np.unravel_index(np.argmax(combined_weights), combined_weights.shape)
            return self.convert_to_move(x, y)

        # Fallback to random valid move
        valid_positions = np.where(valid_moves)
        choice = random.randint(0, len(valid_positions[0]) - 1)
        return self.convert_to_move(valid_positions[0][choice], valid_positions[1][choice])

    def _update_probabilities(self):
        """Update probability distribution based on known information"""
        # Reset probabilities for cells we know about
        self.probability_map[self.known_board != CellState.UNKNOWN] = 0

        # Update based on possible ship placements
        temp_prob = np.zeros_like(self.probability_map)

        for ship_size in self.remaining_ships:
            # Check horizontal placements
            for x in range(self.board_size):
                for y in range(self.board_size - ship_size + 1):
                    if self._can_place_ship(x, y, ship_size, horizontal=True):
                        temp_prob[x, y:y+ship_size] += 1

            # Check vertical placements
            for x in range(self.board_size - ship_size + 1):
                for y in range(self.board_size):
                    if self._can_place_ship(x, y, ship_size, horizontal=False):
                        temp_prob[x:x+ship_size, y] += 1

        # Normalize probabilities
        if temp_prob.sum() > 0:
            self.probability_map = temp_prob / temp_prob.sum()

    def _can_place_ship(self, x: int, y: int, size: int, horizontal: bool) -> bool:
        """Check if a ship of given size can be placed at position"""
        if horizontal:
            ship_cells = self.known_board[x, y:y+size]
        else:
            ship_cells = self.known_board[x:x+size, y]

        return len(ship_cells) == size and not any(cell == CellState.MISS for cell in ship_cells)

    def _handle_sunk_ship(self, x: int, y: int):
        """Handle sunk ship notification"""
        # Remove appropriate ship size from remaining ships
        # This is a simplified version - could be made more sophisticated
        if self.remaining_ships:
            self.remaining_ships.pop()

    def _learn_from_game(self):
        """Learn from completed game"""
        for move, result in self.current_game_moves:
            if move not in self.success_patterns:
                self.success_patterns[move] = 0
            # Update success rate
            if "hit" in result:
                self.success_patterns[move] += self.learning_rate
            else:
                self.success_patterns[move] -= self.learning_rate * 0.5

        self._save_patterns()
        self.current_game_moves = []

    def _load_patterns(self) -> dict:
        """Load learned patterns from file"""
        try:
            with open('battleship_patterns.pkl', 'rb') as f:
                return pickle.load(f)
        except FileNotFoundError:
            return {}

    def _save_patterns(self):
        """Save learned patterns to file"""
        with open('battleship_patterns.pkl', 'wb') as f:
            pickle.dump(self.success_patterns, f)