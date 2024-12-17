import socket

SIZE = 9
ROW_LETTERS = "ABCDEFGHI"

class MinesweeperClient:
    def __init__(self):
        self.board = [[False for _ in range(SIZE)] for _ in range(SIZE)]
        self.host = input("Ingrese la dirección IP del servidor: ")
        self.port = int(input("Ingrese el puerto: "))

    def update_adjacent_cells(self, row, col): 
        for i in range(max(0, row-1), min(SIZE, row+2)): 
            for j in range(max(0, col-1), min(SIZE, col+2)): 
                if self.board[i][j] == 0: self.board[i][j] = " " 
                elif isinstance(self.board[i][j], int):
                    continue
    def print_board(self): 
        print("Tablero actual:") 
        print("   " + "".join(f"{i + 1:2}" for i in range(SIZE))) 
        for i in range(SIZE): 
            row_display = ["l" if self.board[i][j] is True else "-" if self.board[i][j] is False else self.board[i][j] for j in range(SIZE)] 
            print(f"{ROW_LETTERS[i]} | " + " ".join(row_display) + " |")

    def parse_move(self, move):
        move = move.strip().upper()
        
        # Determina si es un movimiento de bandera
        flag_action = move.startswith("F ")
        if flag_action:
            move = move[2:]  # Quita el prefijo "F " para obtener solo la celda

        # Validación de longitud de movimiento
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
    def client(self): 
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket: 
            client_socket.connect((self.host, self.port)) 
            print("Conectado al servidor. Ingrese sus movimientos (ejemplo: 'A1' o 'F B2' para banderas).") 
            self.print_board() 
            # Recibir el tablero de minas 
            while True: 
                move = input("Ingrese su movimiento: ") 
                flag_action, row, col = self.parse_move(move)
                if self.board[row][col]:
                    print("No puedes hacer un movimiento sobre una bandera")
                try: 
                    client_socket.sendall(move.encode()) 
     
                    data = client_socket.recv(1024).decode() 
                    if data == "MINA": 
                        print("PERDISTE: Pisaste una mina. Juego terminado.") 
                        break 
                    elif data.isdigit() and int(data) == 0: 
                        self.update_adjacent_cells(row, col) 
                        self.board[row][col] = " " 
                    elif data.isdigit(): 
                        self.board[row][col] = data 
                    elif data == "SB":
                        self.board[row][col] = True
                    else: self.board[row][col] = True 
                    self.print_board() 
                except Exception as e: 
                    print(f"Error inesperado: {e}") 
                    break 
            client_socket.close()
    def client(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket:
            client_socket.connect((self.host, self.port))
            print("Conectado al servidor. Ingrese sus movimientos (ejemplo: 'A1' o 'F B2' para banderas).")
            self.print_board()
            while True:
                move = input("Ingrese su movimiento: ")
                flag_action, row, col = self.parse_move(move)
                try:
                    client_socket.sendall(move.encode())

                    data = client_socket.recv(1024).decode()
                    print(data)
                    if data == "MINA":
                        print("PERDISTE: Pisaste una mina. Juego terminado.")
                        break
                    elif data.isdigit():
                        self.board[row][col] = data
                    else:
                        self.board[row][col] = True
                    
                    self.print_board()
                except Exception as e:
                    print(f"Error inesperado: {e}") 
                    break
            client_socket.close()
if __name__ == '__main__':
    game_client = MinesweeperClient()
    game_client.client()
