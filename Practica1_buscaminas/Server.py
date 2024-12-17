import socket
import random

SIZE = 9
NUM_MINES = 10
ROW_LETTERS = "ABCDEFGHI"

class MinesweeperServer:
    def __init__(self):
        self.mines = [[False for _ in range(SIZE)] for _ in range(SIZE)]
        self.flags = [[False for _ in range(SIZE)] for _ in range(SIZE)]
        self.place_mines()
        self.print_mine_board()

    def place_mines(self):
        placed = 0
        while placed < NUM_MINES:
            row = random.randint(0, SIZE - 1)
            col = random.randint(0, SIZE - 1)
            if not self.mines[row][col]:
                self.mines[row][col] = True
                placed += 1

    def print_mine_board(self):
        """Imprime el tablero de minas en la consola del servidor."""
        print("Tablero de Minas:")
        print("   " + "".join(f"{i + 1:2}" for i in range(SIZE)))
        for i in range(SIZE):
            row_display = [("*" if self.mines[i][j] else "-") for j in range(SIZE)]
            print(f"{ROW_LETTERS[i]} | " + " ".join(row_display) + " |")
        print()

    def check_cell(self, row, col):
        if self.mines[row][col]:
            return "MINA"
        count = self.count_adjacent_mines(row, col)
        return str(count)

    def count_adjacent_mines(self, row, col):
        count = 0
        for i in range(-1, 2):
            for j in range(-1, 2):
                nr, nc = row + i, col + j
                if 0 <= nr < SIZE and 0 <= nc < SIZE and self.mines[nr][nc]:
                    count += 1
        return count

    def parse_move(self, move):
        """Interpreta el movimiento ingresado."""
        # Quitar espacios innecesarios y convertir en mayúsculas
        move = move.strip().upper()
        
        flag_action = move.startswith("F ")
        if flag_action:
            move = move[2:]  # Remover el prefijo "F "

        # Validar que el movimiento tenga al menos 2 caracteres (letra y número)
        if len(move) < 2:
            raise ValueError("Formato de movimiento inválido")

        # Obtener la fila y columna
        row = ROW_LETTERS.find(move[0])
        try:
            col = int(move[1:]) - 1
        except ValueError:
            raise ValueError("Columna inválida en el movimiento")

        # Validar si la fila y columna están dentro del rango
        if row == -1 or not (0 <= col < SIZE):
            raise ValueError("Movimiento fuera de rango")

        return flag_action, row, col

    def toggle_flag(self, row, col):
        """Coloca o quita una bandera en la posición dada."""
        self.flags[row][col] = not self.flags[row][col]
        return "B" if self.flags[row][col] else "SB"

def handle_client(conn, addr):
    print(f"Conectado a {addr}")
    game = MinesweeperServer()

    while True:
        data = conn.recv(1024).decode().strip()
        if not data:
            continue
        try:
            flag_action, row, col = game.parse_move(data)
            
            if flag_action:
                result = game.toggle_flag(row, col)
            else:
                result = game.check_cell(row, col)
            # Verificar si el resultado indica que el jugador ha pisado una mina
            if result == "MINA":
                conn.sendall(result.encode())
                print(result)
                break
            

            # Imprimir en la consola del servidor el movimiento del jugador y el resultado
            print(f"Movimiento del jugador: {data} -> Resultado: {result}")

            conn.sendall(result.encode())
        except ValueError as e:
            print(f"Error en el movimiento: {e}")
            conn.sendall("ERROR: Movimiento inválido".encode())

if __name__ == '__main__':
    HOST = "127.0.0.1"
    PORT = 8081

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
        server_socket.bind((HOST, PORT))
        server_socket.listen()
        print("Servidor listo y esperando conexiones...")

        while True:
            conn, addr = server_socket.accept()
            handle_client(conn, addr)
            conn.close()
            break