import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    public static final String SPACE = " ";
    public static final String KNIGHT = "KNIGHT";
    public static final String ARCHER = "ARCHER";
    public static final String GIANT = "GIANT";
    public static final int ARCHER_COST = 100;
    public static final int GIANT_COST = 140;
    public static final int KNIGHT_COST = 80;
    public static final int KNIGHT_THRESOLD = 8;
    public static final int KNIGHT_CLOSE_THRESOLD = 3;
    public static final int ARCHER_THRESOLD = 2;
    public static final int GIANT_THRESOLD = 1;
    public static final int MINE_UPGRADE_THRESOLD = 3;
    static final int QUEEN_AGRESSION_TRIGGER = 160;
    static final int TOWER_THRESOLD = 4;
    static final int TOWER_MAX_HP = 800;
    static final int QUEEN_TOWER_REPAIR_HP = 100;

    public static final int DISTANCE_THRESOLD = 50;
    static Located origin = null;



    private void run() {
        Scanner in = new Scanner(System.in);
        int numSites = in.nextInt();
        Sites sites = new Sites();
        for (int i = 0; i < numSites; i++) {
            Site site = new Site(in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt());
            sites.put(site.siteId, site);
        }

        Strategy strategy = new Strategy();

        int turnNumber = 0;

        // game loop
        while (true) {
            turnNumber++;
            int gold = in.nextInt();
            int touchedSite = in.nextInt(); // -1 if none
            System.err.println("touchedSite=" + touchedSite);
            Site touchingSite = sites.get(touchedSite);
            for (int i = 0; i < numSites; i++) {
                int siteId = in.nextInt();
                Site currentSite = sites.get(siteId);
                currentSite.ignore1 = in.nextInt(); // used in future leagues
                currentSite.ignore2 = in.nextInt(); // used in future leagues
                currentSite.structureType = in.nextInt(); // -1 = No structure, 2 = Barracks
                currentSite.owner = in.nextInt(); // -1 = No structure, 0 = Friendly, 1 = Enemy
                currentSite.param1 = in.nextInt();
                currentSite.param2 = in.nextInt();
            }
//            System.err.println("Sites= " + sites);

            int numUnits = in.nextInt();
            Units units = new Units(numUnits);
            for (int i = 0; i < numUnits; i++) {
                Unit unit = new Unit(in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt());
                units.add(unit);
//                System.err.println(unit);
            }

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");


            Commands commands = strategy.chooseCommandsToInvoke(gold, turnNumber, touchingSite, sites, units);
            commands.invoke();

            // First line: A valid queen action
            // Second line: A set of training instructions
//            System.out.println("WAIT");
//            System.out.println("TRAIN");
        }
    }

    public static void main(String args[]) {
        Player player = new Player();
        player.run();
    }
}

class Strategy {

    Commands chooseCommandsToInvoke(int gold, int turnNumber, Site touchingSite, Sites sites, Units units) {
        Commands commands = new Commands();
        CommandHelper myCommandHelper = new CommandHelper();
        Unit myQueen = findQueen(units);
        if (turnNumber == 1) {
            Player.origin = new Located();
            Player.origin.abscisse = myQueen.abscisse;
            Player.origin.ordonnee = myQueen.ordonnee;
        }
        System.err.println("MyQueen : " + myQueen);
        SiteComposition mySiteComposition = new SiteComposition(sites, Site::isMine);
        System.err.println("MysiteComposition : " + mySiteComposition.sites);
        SiteComposition enemySiteComposition = new SiteComposition(sites, Site::isEnemys);
        ArmyComposition enemyArmyComposition = new ArmyComposition(units, Unit::isEnemys);

        //
        if (mySiteComposition.sites.size() < 6) {
            basicBuildingStrategy(commands, myCommandHelper, touchingSite, sites, myQueen, mySiteComposition);
        } else {
            adaptingBuildingStrategy(commands, myCommandHelper, touchingSite, sites, units, myQueen, mySiteComposition, enemySiteComposition, enemyArmyComposition);
        }

        List<Site> productionReadySites = getProductionReadySites(sites);
        ArmyComposition myArmyComposition = new ArmyComposition(units, Unit::isMine);

        spendGoldOnTraining(commands, myCommandHelper, sites, gold, productionReadySites, myArmyComposition, enemyArmyComposition, enemySiteComposition);

        return commands;
    }

    private void basicBuildingStrategy(Commands commands,
                                       CommandHelper myCommandHelper,
                                       Site touchingSite,
                                       Sites sites,
                                       Unit myQueen,
                                       SiteComposition mySiteComposition) {
        int siteId = 0;
        if (touchingSite != null && touchingSite.isNotConstructed()) {
            siteId = touchingSite.siteId;
        } else {
            Optional<Site> closestSite = sites.getClosestSafeNeutralSite(myQueen);
            siteId = closestSite.get().siteId;
        }

        System.err.println("nbSites = " + mySiteComposition.sites.size());
        // construire dans l'ordre : barracks-knights, mine, mine, tower, tower
        switch (mySiteComposition.sites.size()) {
            case 0:
                commands.commandToInvoke1 = myCommandHelper.build(siteId, UnitsType.KNIGHT.mappingToSite.get());
                break;
            case 1:
                commands.commandToInvoke1 = myCommandHelper.build(siteId, SiteType.MINE.name());
                break;
            case 2:
                // is MineUpgraded ? build next mine : upgrade current mine
                if (isGoldMineEnoughUpgraded(touchingSite)) {
                    commands.commandToInvoke1 = myCommandHelper.build(siteId, SiteType.MINE.name());
                }
                else {
                    commands.commandToInvoke1 = myCommandHelper.build(touchingSite.siteId, SiteType.MINE.name());
                }
                break;
            case 3:
                commands.commandToInvoke1 = myCommandHelper.build(siteId, SiteType.TOWER.name());
                break;
            case 4:
                commands.commandToInvoke1 = myCommandHelper.build(siteId, SiteType.TOWER.name());
                break;
            case 5:
                commands.commandToInvoke1 = myCommandHelper.build(siteId, UnitsType.ARCHER.mappingToSite.get());
                break;
            case 6:
                commands.commandToInvoke1 = myCommandHelper.build(siteId, UnitsType.GIANT.mappingToSite.get());
                break;
            default:
                siteId = sites.findClosestUpgradableTower(myQueen).map(site -> site.siteId).orElse(-1);
                if (siteId != -1) {
                    commands.commandToInvoke1 = myCommandHelper.build(siteId, SiteType.TOWER.name());
                }
                else {
                    commands.commandToInvoke1 = CommandType.WAIT.name();
                }
        }
    }

    private boolean isGoldMineEnoughUpgraded(Site touchingSite) {
        return touchingSite == null || touchingSite.param1 >= touchingSite.ignore2;
    }

    private void adaptingBuildingStrategy(Commands commands, CommandHelper myCommandHelper, Site touchingSite, Sites sites,
                                          Units units, Unit myQueen, SiteComposition mySiteComposition,
                                          SiteComposition enemySiteComposition, ArmyComposition enemyArmy) {
        System.err.println("adapting");

        // What should i build next
        int siteDestinationId = -1;
        // if queen in range of enemy turret, go to origin
        if (sites.isInRangeOfEnemyTurrets(myQueen)) {
            System.err.println("should go to safety");
            siteDestinationId = 0;
            commands.commandToInvoke1 = myCommandHelper.moveToPosition(Player.origin.abscisse, Player.origin.ordonnee);
        }

        // if queen in range of knight, go to closest tower
        if (units.isInRangeOfEnemyKnigths(myQueen)) {
            System.err.println("going under turrer protection && upgrade");
            siteDestinationId = sites.findClosestUpgradableTower(myQueen).map(site -> site.siteId).orElse(-1);
            if (siteDestinationId != -1) {
                commands.commandToInvoke1 = myCommandHelper.build(siteDestinationId, SiteType.TOWER.name());
            }
        }

        // si nb mines < 2
        if (siteDestinationId == -1 && mySiteComposition.nbMines < 2) {
            System.err.println("should building mines");
            Optional<Site> mineToUp = sites.findClosestSafeGoldMineToBuild(myQueen);
            siteDestinationId = mineToUp.map(site -> site.siteId).orElse(-1);
            if (siteDestinationId != -1) {
                Site destination = sites.get(siteDestinationId);
                commands.commandToInvoke1 = myCommandHelper.build(siteDestinationId, SiteType.MINE.name());
            }
        }
        // 2 barracks knight / 1 archer / 1 giants
        if (siteDestinationId == -1 && mySiteComposition.nbKnightBarracks < 2) {
            System.err.println("should building knight barracks");
            siteDestinationId = sites.getClosestSafeNeutralSite(myQueen).map(site -> site.siteId).orElse(-1);
            if (siteDestinationId != -1) {
                commands.commandToInvoke1 = myCommandHelper.build(siteDestinationId, UnitsType.KNIGHT.mappingToSite.get());
            }
        }
        if (siteDestinationId == -1 && mySiteComposition.nbArcherBarracks < 1) {
            System.err.println("should building archer barracks");
            siteDestinationId = sites.getClosestSafeNeutralSite(myQueen).map(site -> site.siteId).orElse(-1);
            if (siteDestinationId != -1) {
                commands.commandToInvoke1 = myCommandHelper.build(siteDestinationId, UnitsType.ARCHER.mappingToSite.get());
            }
        }
        if (siteDestinationId == -1 && mySiteComposition.nbGiantBarracks < 1) {
            System.err.println("should building giant barracks");
            siteDestinationId = sites.getClosestSafeNeutralSite(myQueen).map(site -> site.siteId).orElse(-1);
            if (siteDestinationId != -1) {
                commands.commandToInvoke1 = myCommandHelper.build(siteDestinationId, UnitsType.GIANT.mappingToSite.get());
            }
        }
        int myIncome = mySiteComposition.income();
        int hisIncome = enemySiteComposition.income();
        System.err.println("incomes : " + myIncome + " " + hisIncome);
        if (siteDestinationId == -1 && myIncome < hisIncome) {
            // find an upgradable mine close or neutral close
            System.err.println("should increase income");
            siteDestinationId = sites.findClosestSafeUpgradableMine(myQueen).map(site -> site.siteId).orElse(-1);
            if (siteDestinationId != -1) {
                commands.commandToInvoke1 = myCommandHelper.build(siteDestinationId, SiteType.MINE.name());
            }
        }
        // Si enemy proches -> Tower
        if (siteDestinationId == -1 && mySiteComposition.nbTowers < mySiteComposition.nbMines + 1) {
            // find an upgradable tower close or neutral close
            System.err.println("should build tower");
            siteDestinationId = sites.findClosestUpgradableTower(myQueen).map(site -> site.siteId).orElse(-1);
            if (siteDestinationId != -1) {
                commands.commandToInvoke1 = myCommandHelper.build(siteDestinationId, SiteType.TOWER.name());
            }
        }

        // amélioration des mines existantes
        if (siteDestinationId == -1) {
            System.err.println("should upgrade existing mines");
            siteDestinationId = sites.findClosestSafeUpgradableMine(myQueen).map(site -> site.siteId).orElse(-1);
            if (siteDestinationId != -1) {
                commands.commandToInvoke1 = myCommandHelper.build(siteDestinationId, SiteType.MINE.name());
            }
        }

        // Crée des nouvelles mines
        if (siteDestinationId == -1) {
            System.err.println("should build more mines");
            siteDestinationId = sites.findClosestSafeGoldMineToBuild(myQueen).map(site -> site.siteId).orElse(-1);
            if (siteDestinationId != -1) {
                commands.commandToInvoke1 = myCommandHelper.build(siteDestinationId, SiteType.MINE.name());
            }
        }



        if (siteDestinationId == -1) {
            commands.commandToInvoke1 = CommandType.WAIT.toString();
        }

//        String buildingType = computeNextBuildingType(sites);
//        if (siteCanBeBuilt(touchingSite)) {
//            commands.commandToInvoke1 = myCommandHelper.build(touchingSite.siteId, buildingType);
//        } else {
//            Optional<Site> closestSite = sites.getClosestSafeNeutralSite(myQueen);
//            if (closestSite.isPresent()) {
//                commands.commandToInvoke1 = computeMoveCommand(myCommandHelper, myQueen, closestSite.get());
//            } else {
//                commands.commandToInvoke1 = CommandType.WAIT.toString();
//            }
//        }
    }

    public int canProduceUnitType(List<Site> productionReadySites, UnitsType unitsType) {
        Optional<Site> productionSite = productionReadySites.stream().filter(site -> site.canBuild(unitsType)).findFirst();
        return productionSite.map(site -> site.siteId).orElse(-1);
    }

    public void spendGoldOnTraining(Commands commands, CommandHelper myCommandHelper,
                                     Sites sites, int remainingGold, List<Site> productionReadySites,
                                     ArmyComposition myArmyComposition, ArmyComposition enemyArmyComposition,
                                     SiteComposition enemySiteComposition) {
        List<Integer> ids = new ArrayList<>();
        int i = 0;

        int goldToEconomize = 0;
        System.err.println("remainingGold = " + remainingGold);
        System.err.println("enemyArmy = " + enemyArmyComposition);

        // si knight > 8 -> archers
        if (enemyArmyComposition.nbKnights >= Player.KNIGHT_THRESOLD ) {
            System.err.println("need Archers");
            if (!canAffordArcher(remainingGold)) {
                goldToEconomize = Player.ARCHER_COST;
            }
            else {
                int buildingId = canProduceUnitType(productionReadySites, UnitsType.ARCHER);
                if (buildingId != -1) {
                    ids.add(buildingId);
                    remainingGold -= Player.ARCHER_COST;
                }
            }
        }
        // si enemy tower > 5 -> giants
        if (enemySiteComposition.nbTowers > Player.TOWER_THRESOLD ) {
            System.err.println("need Giants");
            if (!canAffordGiant(remainingGold)) {
                goldToEconomize = Player.GIANT_COST;
            }
            else {
                int buildingId = canProduceUnitType(productionReadySites, UnitsType.GIANT);
                if (buildingId != -1) {
                    ids.add(buildingId);
                    remainingGold -= Player.GIANT_COST;
                }
            }
        }

        // sinon knights
        int knightBuildingId = canProduceUnitType(productionReadySites, UnitsType.KNIGHT);
        if ((remainingGold - Player.KNIGHT_COST > goldToEconomize) && remainingGold > Player.KNIGHT_COST && knightBuildingId != -1) {
            System.err.println("need Knights");
            ids.add(knightBuildingId);
            remainingGold -= Player.KNIGHT_COST;

            knightBuildingId = canProduceUnitType(productionReadySites, UnitsType.KNIGHT);
        }


//        for (Site availableSite : productionReadySites) {
//
//            if (availableSite.isKnigthsBarracks() && canAffordKnight(remainingGold) && myArmyComposition.nbKnights < enemyArmyComposition.nbKnights) {
//                ids.add(availableSite.siteId);
//                remainingGold -= Player.KNIGHT_COST;
//            } else if (availableSite.isArchersBarracks() && canAffordArcher(remainingGold) && myArmyComposition.nbArchers < Player.ARCHER_THRESOLD) {
//                ids.add(availableSite.siteId);
//                remainingGold -= Player.ARCHER_COST;
//            } else if (availableSite.isGiantsBarracks() && canAffordGiant(remainingGold) && myArmyComposition.nbGiants < Player.GIANT_THRESOLD) {
//                ids.add(availableSite.siteId);
//                remainingGold -= Player.GIANT_COST;
//            }
//        }

        commands.commandToInvoke2 = myCommandHelper.train(ids);
    }

    private String computeMoveCommand(CommandHelper myCommandHelper, Unit myQueen, Site closestSite) {
        return myCommandHelper.moveTowardSite(closestSite);
    }

    private UnitsType computeNextBarrackType(long nbKnightsBarracks, long nbArchersBarracks) {
        return (nbKnightsBarracks <= nbArchersBarracks) ? UnitsType.KNIGHT : UnitsType.ARCHER;
    }

    private Unit findQueen(Units units) {
        return units.stream().filter(
                unit -> unit.isQueen() && unit.isMine()
        ).findFirst().get();
    }

    private boolean canAffordArcher(int remainingGold) {
        return remainingGold > Player.ARCHER_COST;
    }

    private boolean canAffordGiant(int remainingGold) {
        return remainingGold > Player.GIANT_COST;
    }

    private boolean canAffordKnight(int remainingGold) {
        return remainingGold > Player.KNIGHT_COST;
    }

    private boolean siteCanBeBuilt(Site touchingSite) {
        return touchingSite != null && touchingSite.isNotConstructed();
    }

    private long countArchersBarracks(Sites sites) {
        return sites.values().stream().filter(
                site -> site.isArchersBarracks() && site.isMine()
        ).count();
    }

    private long countKnightsBarracks(Sites sites) {
        return sites.values().stream().filter(
                site -> site.isKnigthsBarracks() && site.isMine()
        ).count();
    }

    private List<Site> getProductionReadySites(Sites sites) {
        return sites.values().stream().filter(Site::isProductionReady).collect(Collectors.toList());
    }

    private String computeNextBuildingType(Sites sites) {
        List<Site> mySites = sites.values().stream().filter(Site::isMine).collect(Collectors.toList());
        long nbArcherBarracks = countArchersBarracks(mySites);
        long nbKnightBarracks = countKnightsBarracks(mySites);
        long nbGiantsBarracks = countGiantsBarracks(mySites);
        long nbTowers = countTowers(mySites);

        BuildingComposition buildingComposition = new BuildingComposition(nbArcherBarracks, nbKnightBarracks, nbGiantsBarracks, nbTowers);
        String buildingType = "";
        if (buildingComposition.shouldBuildKnightBarracks()) {
            buildingType = UnitsType.KNIGHT.mappingToSite.get();
        } else if (buildingComposition.shouldBuildArcherBarracks()) {
            buildingType = UnitsType.ARCHER.mappingToSite.get();
        } else if (buildingComposition.shouldBuildGiantsBarrack()) {
            buildingType = UnitsType.GIANT.mappingToSite.get();
        } else /*if (nbTowers < 5) */ {
            buildingType = SiteType.TOWER.toString();
        } /*else {
                buildingType = "";
            }*/

        return buildingType;
    }

    private long countGiantsBarracks(List<Site> mySites) {
        return mySites.stream().filter(Site::isGiantsBarracks).count();
    }

    private long countTowers(List<Site> mySites) {
        return mySites.stream().filter(Site::isTower).count();
    }

    private long countKnightsBarracks(List<Site> mySites) {
        return mySites.stream().filter(Site::isKnigthsBarracks).count();
    }

    private long countArchersBarracks(List<Site> mySites) {
        return mySites.stream().filter(Site::isArchersBarracks).count();
    }
}

class Possession {
    int owner;

    boolean isMine() {
        return owner == 0;
    }

    public boolean isEnemys() {
        return owner == 1;
    }

    public boolean isNeutral() {
        return owner == -1;
    }
}

class Located extends Possession {
    int abscisse;
    int ordonnee;
    public Double distanceToUnit(Located unit) {
        return Math.sqrt(
                Math.pow(unit.abscisse - abscisse, 2) + Math.pow(unit.ordonnee - ordonnee, 2)
        );
    }
}

class Site extends Located {
    int siteId;
    int radius;
    int structureType;
    int param1;
    int param2;
    int ignore1;
    int ignore2;

    Site(int siteId, int abscisse, int ordonnee, int radius) {
        this.siteId = siteId;
        this.abscisse = abscisse;
        this.ordonnee = ordonnee;
        this.radius = radius;
    }

    @Override
    public String toString() {
        return "Site{" +
                "owner=" + owner +
                ", siteId=" + siteId +
                ", abscisse=" + abscisse +
                ", ordonnee=" + ordonnee +
                ", radius=" + radius +
                ", structureType=" + structureType +
                ", param1=" + param1 +
                ", param2=" + param2 +
                ", ignore1=" + ignore1 +
                ", ignore2=" + ignore2 +
                "}\n";
    }

    public boolean isNotConstructed() {
        return structureType == -1;
    }

    public boolean isBarracks() {
        return structureType == 2;
    }

    public boolean isTower() {
        return structureType == 1;
    }

    public boolean isGoldMine()  {
        return structureType == 0;
    }

    public boolean isArchersBarracks() {
        return isBarracks() && param2 == 1;
    }

    public boolean isKnigthsBarracks() {
        return isBarracks() && param2 == 0;
    }

    public boolean isGiantsBarracks() {
        return isBarracks() && param2 == 2;
    }

    public boolean isProductionReady() {
        return isBarracks() && isMine() && param1 == 0;
    }

    public boolean canBuild(UnitsType unitsType) {
        return isBarracks() && param2 == unitsType.barrackType;
    }

    public boolean isGoldMineUpgradable() {
        return isGoldMine() && ignore2 > param1;
    }

    public boolean inRange(Located unit) {
        // (x - abs)² + ( y -ord)² <= R²
        // Only for turrets
        return isTower() && distanceToUnit(unit) <= param2;
    }
}

class BuildingComposition {
    long nbArcherBarracks;
    long nbKnightBarracks;
    long nbGiantsBarracks;
    long nbTowers;

    public BuildingComposition(long nbArcherBarracks, long nbKnightBarracks, long nbGiantsBarracks, long nbTowers) {
        this.nbArcherBarracks = nbArcherBarracks;
        this.nbKnightBarracks = nbKnightBarracks;
        this.nbGiantsBarracks = nbGiantsBarracks;
        this.nbTowers = nbTowers;
    }

    boolean shouldBuildKnightBarracks() {
        return nbKnightBarracks == 0 || (nbKnightBarracks == 1 && nbTowers >= 2);
    }

    boolean shouldBuildArcherBarracks() {
        return nbArcherBarracks == 0 && (nbTowers >= 2 && nbKnightBarracks >= 2);
    }

    boolean shouldBuildGiantsBarrack() {
        return nbGiantsBarracks == 0 && (nbTowers >= 2 && nbKnightBarracks >= 2 && nbArcherBarracks >= 1);
    }

    boolean shouldBuildTower() {
        return (nbKnightBarracks == 1 && nbTowers < 2) || (nbKnightBarracks == 2 && nbArcherBarracks == 1 && nbGiantsBarracks == 1);
    }
}

class SiteComposition {
    List<Site> sites;
    long nbMines;
    long nbTowers;
    long nbKnightBarracks;
    long nbArcherBarracks;
    long nbGiantBarracks;

    public SiteComposition(Sites armySites, Predicate<Site> filter) {
        sites = armySites.values().stream().filter(filter).collect(Collectors.toList());
        nbKnightBarracks = sites.stream().filter(Site::isKnigthsBarracks).count();
        nbTowers = sites.stream().filter(Site::isTower).count();
        nbGiantBarracks = sites.stream().filter(Site::isGiantsBarracks).count();
        nbArcherBarracks = sites.stream().filter(Site::isArchersBarracks).count();
        nbMines = sites.stream().filter(Site::isGoldMine).count();
    }

    public String basicStrategyNextBuilding() {
        switch (sites.size()) {
            case 0 :
                return UnitsType.KNIGHT.mappingToSite.get();
            case 1 :
                return SiteType.MINE.name();
            case 2 :
                return SiteType.MINE.name();
            case 3 :
                return SiteType.TOWER.name();
            case 4 :
                return SiteType.TOWER.name();
            case 5 :
                return UnitsType.ARCHER.mappingToSite.get();
            case 6 :
                return UnitsType.GIANT.mappingToSite.get();
        }
        return "";
    }

    public int income() {
        int income = 0;

        List<Site> goldmines = sites.stream().filter(Site::isGoldMine).collect(Collectors.toList());
        for (Site site : goldmines) {
            income += site.param1;
        }
        return income;
    }
}

class ArmyComposition {
    long nbKnights;
    long nbArchers;
    long nbGiants;
    List<Unit> armyUnits;

    public ArmyComposition(Units units, Predicate<Unit> filter) {
        armyUnits = units.stream().filter(filter).collect(Collectors.toList());
        nbArchers = armyUnits.stream().filter(Unit::isArcher).count();
        nbKnights = armyUnits.stream().filter(Unit::isKnight).count();
        nbGiants = armyUnits.stream().filter(Unit::isGiant).count();
    }

    @Override
    public String toString() {
        return "ArmyComposition{" +
                "nbKnights=" + nbKnights +
                ", nbArchers=" + nbArchers +
                ", nbGiants=" + nbGiants +
                '}';
    }

    public long computeNbKnightCloseToUnit(Unit unit) {
        return armyUnits.stream().filter(Unit::isKnight).filter(knight -> knight.distanceToUnit(unit) <= Player.DISTANCE_THRESOLD).count();
    }
}

class Sites extends HashMap<Integer, Site> {
    public Optional<Site> findClosestUpgradableTower(Unit unit) {
        return this.values().
                stream().filter(site -> site.isNotConstructed() || (site.isMine() && site.isTower())).
                filter(tower -> tower.param1 < Player.TOWER_MAX_HP - Player.QUEEN_TOWER_REPAIR_HP).
                sorted(Comparator.comparing(site -> site.distanceToUnit(unit))).findFirst();

//            List<Site> towers = this.values().stream().filter(Site::isMine).filter(Site::isTower).collect(Collectors.toList());
//
//            towers = towers.stream().filter(tower -> tower.param1 < Player.TOWER_MAX_HP - Player.QUEEN_TOWER_REPAIR_HP).collect(Collectors.toList());
//            towers.sort(Comparator.comparing(site -> site.distanceToUnit(unit)));
//
//            return Optional.of(towers.get(0));
    }

    public Optional<Site> findClosestSafeUpgradableMine(Unit unit) {
        return this.values().
                stream().filter(Site::isGoldMineUpgradable).
                filter(mine -> mine.param1 < mine.ignore2).
                filter(mine -> !isInRangeOfEnemyTurrets(mine)).
                sorted(Comparator.comparing(site -> site.distanceToUnit(unit))).findFirst();
    }

    public Optional<Site> findClosestSafeGoldMineToBuild(Unit myQueen) {
        return this.values().stream().
                filter(mine -> mine.isGoldMineUpgradable() || mine.isNotConstructed()).
                filter(mine -> !isInRangeOfEnemyTurrets(mine)).
                sorted(Comparator.comparing(site -> site.distanceToUnit(myQueen))).findFirst();
    }

    public Optional<Site> getClosestSafeNeutralSite(Unit myQueen) {
        return this.values().stream().
                filter(Site::isNotConstructed).
                filter(site -> !isInRangeOfEnemyTurrets(site)).
                sorted(Comparator.comparing(site -> site.distanceToUnit(myQueen))).
                findFirst();
    }

    public boolean isInRangeOfEnemyTurrets(Located unit) {
        return this.values().stream().filter(Site::isTower).
                filter(Site::isEnemys).
                anyMatch(site -> site.inRange(unit));
    }
}

class Units extends ArrayList<Unit> {
    Units(int numUnits) {
        super(numUnits);
    }

    public boolean isInRangeOfEnemyKnigths(Located unit) {
        return this.stream().filter(unit1 -> unit1.isEnemys() && unit1.isKnight()).
                anyMatch(knight -> knight.distanceToUnit(unit) <= Player.QUEEN_AGRESSION_TRIGGER);
    }
}

class Unit extends Located {
    int unitType;
    int health;

    Unit(int abscisse, int ordonnee, int owner, int unitType, int health) {
        this.abscisse = abscisse;
        this.ordonnee = ordonnee;
        this.owner = owner;
        this.unitType = unitType;
        this.health = health;
    }

    @Override
    public String toString() {
        return "Unit{" +
                "owner=" + owner +
                ", abscisse=" + abscisse +
                ", ordonnee=" + ordonnee +
                ", unitType=" + (isQueen() ? "Queen" : isKnight() ? "Knight" : isArcher() ? "Archer" : "Giant") +
                ", health=" + health +
                '}';
    }

    boolean isQueen() {
        return unitType == -1;
    }

    public boolean isKnight() {
        return unitType == 0;
    }

    public boolean isArcher() {
        return unitType == 1;
    }

    public boolean isGiant() {
        return unitType == 2;
    }
}

class Commands {
    String commandToInvoke1;
    String commandToInvoke2;

    public String getCommandToInvoke1() {
        return commandToInvoke1;
    }

    public String getCommandToInvoke2() {
        return commandToInvoke2;
    }

    void invoke() {
        System.out.println(commandToInvoke1);
        System.out.println(commandToInvoke2);
    }
}

class CommandHelper {
    String train(List<Integer> ids) {
        StringBuilder base = new StringBuilder(CommandType.TRAIN.toString());
        for (int id : ids) {
            if (id >= 0) {
                base.append(Player.SPACE).append(id);
            }
        }

        return base.toString();
    }

    String build(int siteId, String buildingType) {
        if (buildingType == null || "".equals(buildingType)) {
            return CommandType.WAIT.toString();
        }
        return CommandType.BUILD.toString() + Player.SPACE + siteId + Player.SPACE + buildingType;
    }

    String moveTowardSite(Site site) {
        return CommandType.MOVE + Player.SPACE + site.abscisse + Player.SPACE + site.ordonnee;
    }

    String moveToPosition(int abscisse, int ordonnee) {
        return CommandType.MOVE + Player.SPACE + abscisse + Player.SPACE + ordonnee;
    }
}

enum CommandType {
    WAIT,
    MOVE,
    BUILD,
    TRAIN;
}

enum UnitsType {
    KNIGHT(Player.KNIGHT, 0, () -> SiteType.BARRACKS + "-" + Player.KNIGHT),
    ARCHER(Player.ARCHER, 1, () -> SiteType.BARRACKS + "-" + Player.ARCHER),
    GIANT(Player.GIANT, 2, () -> SiteType.BARRACKS + "-" + Player.GIANT);
    String name;
    // to check on Site.param2 value
    int barrackType;
    Supplier<String> mappingToSite;

    UnitsType(String name, int barrackType, Supplier<String> mappingToSite) {
        this.name = name;
        this.barrackType = barrackType;
        this.mappingToSite = mappingToSite;
    }
}

enum SiteType {
    TOWER,
    BARRACKS,
    MINE;
}