package io.github.maniadiaz.bedroom;

/** Stateless layout shared by generation and connectivity tests. Coordinates include negative chunks. */
final class Level0Layout {
    private final long seed;
    Level0Layout(long seed) { this.seed = seed; }

    long hash(int x, int z, int salt) {
        long n = seed ^ ((long) x * 0x632be59bd9b4e019L) ^ ((long) z * 0x9e3779b97f4a7c15L) ^ salt;
        n = (n ^ (n >>> 30)) * 0xbf58476d1ce4e5b9L;
        n = (n ^ (n >>> 27)) * 0x94d049bb133111ebL;
        return n ^ (n >>> 31);
    }
    int template(int cellX, int cellZ) { return Math.floorMod(hash(cellX, cellZ, 37), 5); }
    boolean supplyCell(int cellX, int cellZ) { return Math.floorMod(cellX, 12) == 5 + Math.floorMod(hash(Math.floorDiv(cellX, 12), cellZ, 89), 3); }
    boolean supply(int x, int z) { return Math.floorMod(x, 16) == 3 && Math.floorMod(z, 16) == 11 && supplyCell(Math.floorDiv(x, 16), Math.floorDiv(z, 16)); }
    boolean exit(int x, int z) { return Math.floorMod(x, 128) == 115 && Math.floorMod(z, 128) == 115; }
    boolean wet(int x, int z) {
        // Coherent patches near boundaries, rather than independently random pixels.
        return (Math.floorMod(x, 16) < 3 || Math.floorMod(z, 16) < 3)
                && Math.floorMod(hash(Math.floorDiv(x, 4), Math.floorDiv(z, 4), 81), 5) == 0;
    }
    boolean lamp(int x, int z) { return Math.floorMod(x, 8) == 0 && Math.floorMod(z, 8) == 0; }
    boolean deadLamp(int x, int z) { return Math.floorMod(hash(x, z, 23), 11) == 0; }

    boolean wall(int x, int z) {
        int cx = Math.floorDiv(x, 16), cz = Math.floorDiv(z, 16);
        int dx = Math.floorMod(x, 16), dz = Math.floorMod(z, 16);
        if (exit(x, z)) return true;
        if (dx == 0 && dz == 0) return true;
        // A boundary belongs to one global coordinate; adjacent chunks cannot disagree.
        if (dx == 0) return !(dz >= 7 && dz <= 8) && Math.floorMod(hash(cx, cz, 1), 3) != 0;
        if (dz == 0) return !(dx >= 7 && dx <= 8) && Math.floorMod(hash(cx, cz, 2), 3) != 0;
        // Cross corridors remain open in every template. Other rooms add finite panels or columns.
        if ((dx >= 7 && dx <= 8) || (dz >= 7 && dz <= 8)) return false;
        if (Math.floorMod(cx, 8) == 7 && Math.floorMod(cz, 8) == 7) return false;
        if (supplyCell(cx, cz)) return false;
        return switch (template(cx, cz)) {
            case 0 -> false; // open room
            case 1 -> dx == 4 && dz == 4; // isolated column
            case 2 -> true; // narrow crossing corridors
            case 3 -> (dx == 4 && dz <= 5) || (dz == 12 && dx >= 10); // bends / panels
            default -> (dx == 11 && dz >= 10 && dz <= 14) || (dz == 3 && dx >= 2 && dx <= 5);
        };
    }
}
