import random

class Buscaminas:
    SIZE = 9
    NUM_MINES = 10
    ROW_LETTERS = "ABCDEFGHI"

    def __init__(self):
        self.board = [['-' for _ in range(self.SIZE)] for _ in range(self.SIZE)]
        self.mines = [[False for _ in range(self.SIZE)] for _ in range(self.SIZE)]
        self.revealed = [[False for _ in range(self.SIZE)] for _ in range(self.SIZE)]
        self.flags = [[False for _ in range(self.SIZE)] for _ in range(self.SIZE)]

    def place_mines(self):
        placed = 0
        while placed < self.NUM_MINES:
            row = random.randint(0, self.SIZE - 1)
            col = random.randint(0, self.SIZE - 1)
            if not self.mines[row][col]:
                self.mines[row][col] = True
                placed += 1

    def count_adjacent_mines(self, row, col):
        count = 0
        for i in range(-1, 2):
            for j in range(-1, 2):
                nr, nc = row + i, col + j
                if 0 <= nr < self.SIZE and 0 <= nc < self.SIZE and self.mines[nr][nc]:
                    count += 1
        return count

    def reveal_cell(self, row, col):
        if self.revealed[row][col] or self.flags[row][col]:
            return False
        self.revealed[row][col] = True
        if self.mines[row][col]:
            self.board[row][col] = '*'
            return True  # El jugador perdió al revelar una mina
        count = self.count_adjacent_mines(row, col)
        self.board[row][col] = str(count) if count > 0 else ' '
        if count == 0:
            for i in range(-1, 2):
                for j in range(-1, 2):
                    nr, nc = row + i, col + j
                    if 0 <= nr < self.SIZE and 0 <= nc < self.SIZE:
                        self.reveal_cell(nr, nc)
        return False

    def place_flag(self, row, col):
        if not self.revealed[row][col]:
            self.flags[row][col] = not self.flags[row][col]
            self.board[row][col] = '<l>' if self.flags[row][col] else '-'

    def parse_move(self, move):
        row = self.ROW_LETTERS.find(move[0])
        col = int(move[1:]) - 1
        return row, col

    def get_board_state(self):
        board_str = "  " + " ".join(f"{i+1}" for i in range(self.SIZE)) + "\n"
        for i in range(self.SIZE):
            board_str += f"{self.ROW_LETTERS[i]} " + " ".join(self.board[i]) + "\n"
        return board_str
