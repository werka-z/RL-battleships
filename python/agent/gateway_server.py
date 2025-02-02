from py4j.java_gateway import JavaGateway, CallbackServerParameters
import numpy as np
from .q_agent import QLearningAgent

class BattleshipAgent:
    def __init__(self):
        self.agent = QLearningAgent()
        self.gateway = None

    def connect_to_java(self):
        # Connect to the Java gateway
        self.gateway = JavaGateway(
            callback_server_parameters=CallbackServerParameters()
        )

    def get_next_shot(self, board_state):
        # Convert board state and get next move from Q-learning agent
        row, col = self.agent.choose_action(board_state)
        return row, col

    def update_result(self, row, col, result):
        # Update Q-values based on shot result
        reward = self._get_reward(result)
        self.agent.update(row, col, reward)

    @staticmethod
    def _get_reward(result):
        rewards = {
            "miss": -1,
            "hit": 1,
            "hit and sunk": 2,
            "last ship sunk": 5
        }
        return rewards.get(result, 0)

# Start the agent when this module is run
if __name__ == "__main__":
    agent = BattleshipAgent()
    agent.connect_to_java()