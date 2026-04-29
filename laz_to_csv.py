import laspy
import csv
import sys
import os

def convert_laz_to_csv(input_path, output_path=None, max_points=500000):
    if not os.path.exists(input_path):
        print("Error: no se encontro el archivo " + input_path)
        sys.exit(1)

    if output_path is None:
        output_path = os.path.splitext(input_path)[0] + ".csv"

    print("Leyendo: " + input_path)
    las = laspy.read(input_path)

    x = las.x
    y = las.y
    z = las.z

    total = len(x)
    print("Total de puntos: " + str(total))

    if total > max_points:
        step = total // max_points
        indices = range(0, total, step)
        print("Reduciendo a " + str(max_points) + " puntos")
    else:
        indices = range(total)

    print("Escribiendo CSV: " + output_path)
    with open(output_path, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['x', 'y', 'z'])
        for i in indices:
            writer.writerow([
                round(float(x[i]), 4),
                round(float(y[i]), 4),
                round(float(z[i]), 4)
            ])

    print("Listo. CSV guardado en: " + output_path)
    return output_path

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python laz_to_csv.py archivo.laz")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else None
    convert_laz_to_csv(input_file, output_file)