import csv
import math
import random

def generate_tunnel(output_path="tunnel_deformed.csv",
                    length=30,
                    radius=3.0,
                    points_per_ring=80,
                    rings=200):

    random.seed(99)
    points = []

    for i in range(rings):
        z = (i / rings) * length
        r_variation = radius + random.uniform(-0.05, 0.05)

        # Deformacion critica zona 1 — colapso fuerte
        if 40 < i < 60:
            r_variation = radius * 0.55 + random.uniform(-0.1, 0.1)

        # Deformacion critica zona 2 — abombamiento severo
        if 100 < i < 120:
            r_variation = radius * 1.55 + random.uniform(-0.1, 0.1)

        # Deformacion moderada zona 3
        if 155 < i < 170:
            r_variation = radius * 0.75 + random.uniform(-0.05, 0.05)

        for j in range(points_per_ring):
            angle = (j / points_per_ring) * 2 * math.pi
            noise = random.uniform(-0.03, 0.03)
            r = r_variation + noise
            x = r * math.cos(angle)
            y = r * math.sin(angle)

            if math.pi * 0.85 < angle < math.pi * 1.15:
                y = -radius + random.uniform(0, 0.06)

            points.append((round(x, 4), round(y, 4), round(z, 4)))

    # Escombros en zonas de colapso
    for _ in range(1500):
        x = random.uniform(-radius * 0.6, radius * 0.6)
        z_collapse = random.uniform(6, 9)
        y = -radius + random.uniform(0, 0.8)
        points.append((round(x, 4), round(y, 4), round(z_collapse, 4)))

    print("Generando " + str(len(points)) + " puntos...")

    with open(output_path, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['x', 'y', 'z'])
        for p in points:
            writer.writerow(p)

    print("Listo. CSV guardado en: " + output_path)
    print("Total puntos: " + str(len(points)))
    print("")
    print("Deformaciones simuladas:")
    print("  CRITICA — colapso en z=6 a z=9 (radio reducido 45%)")
    print("  CRITICA — abombamiento en z=15 a z=18 (radio aumentado 55%)")
    print("  ADVERTENCIA — contraccion en z=23 a z=25 (radio reducido 25%)")

if __name__ == "__main__":
    generate_tunnel()