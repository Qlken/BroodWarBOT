import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class TestBot1 extends DefaultBWListener {

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;

    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onUnitCreate(Unit unit) {
        System.out.println("New unit discovered " + unit.getType());
    }

    @Override
    public void onStart() {
        game = mirror.getGame();
        self = game.self();

        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");
        
        int i = 0;
        for(BaseLocation baseLocation : BWTA.getBaseLocations()){
        	System.out.println("Base location #" + (++i) + ". Printing location's region polygon:");
        	for(Position position : baseLocation.getRegion().getPolygon().getPoints()){
        		System.out.print(position + ", ");
        	}
        	System.out.println();
        }

    }

    @Override
    public void onFrame() {
        //game.setTextSize(10);
        game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());
        game.drawTextScreen(10, 230, "Resources: " + self.minerals() + " minerals,  " + self.gas() + " gas");
        StringBuilder units = new StringBuilder("My units:\n");

        //iterate through my units
        for (Unit myUnit : self.getUnits()) {
            units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");

            //if there's enough minerals, train an SCV
            if (myUnit.getType() == UnitType.Terran_Command_Center && self.minerals() >= 50) {
                myUnit.train(UnitType.Terran_SCV);
            }

            //if it's a worker and it's idle, send it to the closest mineral patch
            if (myUnit.getType().isWorker() && myUnit.isIdle()) {
                Unit closestMineral = null;

                //find the closest mineral
                for (Unit neutralUnit : game.neutral().getUnits()) {
                    if (neutralUnit.getType().isMineralField()) {
                        if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
                            closestMineral = neutralUnit;
                        }
                    }
                }

                //if a mineral patch was found, send the worker to gather it
                if (closestMineral != null) {
                    myUnit.gather(closestMineral, false);
                }
            }

            if(myUnit.getType().isWorker() && (self.supplyTotal() - self.supplyUsed()<2) && (self.minerals() >=100))
            {
                TilePosition buildTile = getBuildTile(myUnit,UnitType.Terran_Supply_Depot,self.getStartLocation());
                if(buildTile != null){
                    myUnit.build(UnitType.Terran_Supply_Depot,buildTile);
                    break;
                }
            }

        }




        //draw my units on screen
        game.drawTextScreen(10, 25, units.toString());
    }

    private TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition position)
    {
        TilePosition ret = null;
        int maxDist = 3;
        int stopDist = 40;

        // Refinery, Assimilator, Extractor
        if (buildingType.isRefinery()) {
            for (Unit n : game.neutral().getUnits()) {
                if ((n.getType() == UnitType.Resource_Vespene_Geyser) &&
                        ( Math.abs(n.getTilePosition().getX() - position.getX()) < stopDist ) &&
                        ( Math.abs(n.getTilePosition().getY() - position.getY()) < stopDist )
                        ) return n.getTilePosition();
            }
        }

        while ((maxDist < stopDist) && (ret == null)) {
            for (int i=position.getX()-maxDist; i<=position.getX()+maxDist; i++) {
                for (int j=position.getY()-maxDist; j<=position.getY()+maxDist; j++) {
                    if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false)) {
                        // units that are blocking the tile
                        boolean unitsInWay = false;
                        for (Unit u : game.getAllUnits()) {
                            if (u.getID() == builder.getID()) continue;
                            if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
                        }
                        if (!unitsInWay) {
                            return new TilePosition(i, j);
                        }

                    }
                }
            }
            maxDist += 2;
        }

        if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
        return ret;


    }

    public static void main(String[] args) {
        new TestBot1().run();
    }
}