import matplotlib.pyplot as plt
import numpy as np
import random

class QueenAttack():
    def __init__(self, N, C):
        self.N = N
        self.C = C
        self.board = np.zeros((N, N))

    def random_int(self, start, end):
        return random.randint(start, end)

    def generate_pieces(self, W):
        random.seed(0)

        # generate queens
        queens = 0
        while queens < self.N:
            row, col, color = self.random_int(0, self.N-1), self.random_int(0, self.N-1), self.random_int(1, self.C)
            if self.board[row, col] != 0:
                continue

            self.board[row, col] = color
            queens += 1

        # generate walls
        walls = 0
        while walls < W:
            row, col, color = self.random_int(0, self.N-1), self.random_int(0, self.N-1), -1
            if self.board[row, col] != 0:
                continue

            self.board[row, col] = color
            walls += 1

    def generate_queen_pieces(self, x1, y1):
        attack_color = 1

        # vertically
        for x2 in range(0, self.N):
            self.board[x2, y1] = attack_color

        # horizontally
        for y2 in range(0, self.N):
            self.board[x1, y2] = attack_color

        # diagonally, down-left
        x2 = x1+1
        y2 = y1-1
        while x2 < self.N and y2 >= 0:
            self.board[x2, y2] = attack_color
            x2 += 1
            y2 -= 1

        # diagonally, down-right
        x2 = x1+1
        y2 = y1+1
        while x2 < self.N and y2 < self.N:
            self.board[x2, y2] = attack_color
            x2 += 1
            y2 += 1

        # diagonally, up-left
        x2 = x1-1
        y2 = y1-1
        while x2 >= 0 and y2 >= 0:
            self.board[x2, y2] = attack_color
            x2 -= 1
            y2 -= 1

        # diagonally, up-right
        x2 = x1-1
        y2 = y1+1
        while x2 >= 0 and y2 < self.N:
            self.board[x2, y2] = attack_color
            x2 -= 1
            y2 += 1

        # wall borders
        for x2 in range(0, self.N):
            self.board[x2, 0] = -1
            self.board[x2, self.N-1] = -1

        for y2 in range(0, self.N):
            self.board[0, y2] = -1
            self.board[self.N-1, y2] = -1
        
        self.board[x1, y1] = self.C


    def queen_attack(self, x1, y1):
        pairs = []

        pair1 = (x1, y1)

        # vertically, down
        for x2 in range(x1+1, self.N):
            if self.board[x2, y1] == 0:
                continue

            if self.board[x2, y1] == self.board[x1, y1]:
                pairs.append([pair1, (x2, y1)])

            break

        # horizontally, right
        for y2 in range(y1+1, self.N):
            if self.board[x1, y2] == 0:
                continue

            if self.board[x1, y2] == self.board[x1, y1]:
                pairs.append([pair1, (x1, y2)])

            break

        # diagonally, down-left
        x2 = x1+1
        y2 = y1-1
        while x2 < self.N and y2 >= 0:
            if self.board[x2, y2] == 0:
                x2 += 1
                y2 -= 1
                continue

            if self.board[x2, y2] == self.board[x1, y1]:
                pairs.append([pair1, (x2, y2)])

            break

        # diagonally, down-right
        x2 = x1+1
        y2 = y1+1
        while x2 < self.N and y2 < self.N:
            if self.board[x2, y2] == 0:
                x2 += 1
                y2 += 1
                continue

            if self.board[x2, y2] == self.board[x1, y1]:
                pairs.append([pair1, (x2, y2)])

            break

        return pairs

    def queen_attack_pairs(self):
        pairs = []

        for x1 in range(0, self.N):
            for y1 in range(0, self.N):
                if self.board[x1, y1] <= 0:
                    continue

                pairs += self.queen_attack(x1, y1)

        return pairs

    def plot_board(self, file_name):
        # Draw the pieces
        im = plt.imshow(self.board, cmap='viridis')

        tick_values = np.arange(-1, self.C+1)
        cbar = plt.colorbar(im, ticks=tick_values)
        tick_labels = ['Wall', 'Empty'] + list(map(str, np.arange(1, self.C+1)))
        cbar.ax.set_yticklabels(tick_labels)
        
        # Draw the grid lines
        for i in range(0, self.N+1):
            plt.axhline(i-0.5, color='black', linewidth=1)
            plt.axvline(i-0.5, color='black', linewidth=1)

        # Add row & column numbers
        plt.xticks(np.arange(0, self.N))
        plt.yticks(np.arange(0, self.N))

        # Remove ticks
        plt.tick_params(length=0)

        plt.savefig(file_name, bbox_inches='tight')
        plt.show()

    def draw_chessboard(self, W):
        self.generate_pieces(W=W)

        # Draw pairs under attack
        pairs = self.queen_attack_pairs()
        for pair in pairs:
            x1, y1 = pair[0]
            x2, y2 = pair[1]
            # x is the row in the matrix, it should be the vertical axis
            # y is the column in the matrix, it should be the horizontal axis
            plt.plot([y1, y2], [x1, x2], marker='+', color='white')

        self.plot_board(file_name='QueenAttackBoard.png')

    def draw_queen(self, x, y):
        self.generate_queen_pieces(x, y)

        self.plot_board(file_name='QueenAttackPairs.png')

queen_attack = QueenAttack(N=10, C=3)
#queen_attack.draw_chessboard(W=15)
queen_attack.draw_queen(x=4, y=5)