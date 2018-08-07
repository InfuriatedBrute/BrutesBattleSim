package deprecated;
//package simpleconcepts;
//
//public class TerrainMap {
//	public Terrain[][] grid;
//	
//	public TerrainMap(int xSize, int ySize, MapType type) {
//		grid = new Terrain[xSize][ySize];
//		initialize(type);
//	}
//	
//	public enum MapType{
//		NORMAL;
//	}
//	
//	public enum Terrain {
//		NORMAL;
//	}
//	
//	private void initialize(MapType type) {
//		if(type==MapType.NORMAL) {
//			for(Terrain[] ta : grid) {
//				for(Terrain t : ta) {
//					t = Terrain.NORMAL;
//				}
//			}
//		}
//	}
//
//	public static float getCost(int tx, int ty) {
//		return 1;
//	}
//}