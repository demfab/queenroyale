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
    public static final int KNIGHT_THRESOLD = 16;
    public static final int ARCHER_THRESOLD = 2;
    public static final int GIANT_THRESOLD = 1;

    private void run() {
        Scanner in = new Scanner(System.in);
        int numSites = in.nextInt();
        Sites sites = new Sites();
        for (int i = 0; i < numSites; i++) {
            Site site = new Site(in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt());
            sites.put(site.siteId, site);
        }

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

            Strategy strategy = new Strategy();
            Commands commands = strategy.chooseCommandsToInvoke(gold, touchingSite, sites, units);
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
    Commands chooseCommandsToInvoke(int gold, Site touchingSite, Sites sites, Units units) {
        int remainingGold = gold;
        Commands commands = new Commands();
        CommandHelper myCommandHelper = new CommandHelper();
        Unit myQueen = findQueen(units);

        // What should i build next
        String buildingType = computeNextBuildingType(sites);
        if (siteCanBeBuilt(touchingSite)) {
            commands.commandToInvoke1 = myCommandHelper.build(touchingSite.siteId, buildingType);
        } else {
            Optional<Site> closestSite = getClosestNeutralSite(sites, myQueen);
            if (closestSite.isPresent()) {
                commands.commandToInvoke1 = computeMoveCommand(myCommandHelper, myQueen, closestSite.get());
            } else {
                commands.commandToInvoke1 = CommandType.WAIT.toString();
            }
        }

        List<Site> productionReadySites = getProductionReadySites(sites);
        ArmyComposition myArmyComposition = new ArmyComposition(units, Unit::isMine);
        ArmyComposition enemyArmyComposition = new ArmyComposition(units, Unit::isEnemys);

        SiteComposition mySiteComposition = new SiteComposition(sites, Site::isMine);
        SiteComposition enemySiteComposition = new SiteComposition(sites, Site::isEnemys);

        int ids[] = new int[sites.size()];
        int i = 0;

        for (Site availableSite : productionReadySites) {
            if (availableSite.isKnigthsBarracks() && canAffordKnight(remainingGold) && myArmyComposition.nbKnights < enemyArmyComposition.nbKnights) {
                ids[i++] = availableSite.siteId;
                remainingGold -= Player.KNIGHT_COST;
            } else if (availableSite.isArchersBarracks() && canAffordArcher(remainingGold) && myArmyComposition.nbArchers < Player.ARCHER_THRESOLD) {
                ids[i++] = availableSite.siteId;
                remainingGold -= Player.ARCHER_COST;
            } else if (availableSite.isGiantsBarracks() && canAffordGiant(remainingGold) && myArmyComposition.nbGiants < Player.GIANT_THRESOLD) {
                ids[i++] = availableSite.siteId;
                remainingGold -= Player.GIANT_COST;
            }
        }

        commands.commandToInvoke2 = myCommandHelper.train(ids);

        return commands;
    }

    private String computeMoveCommand(CommandHelper myCommandHelper, Unit myQueen, Site closestSite) {
        return myCommandHelper.moveTowardSite(myQueen, closestSite);
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

    private Optional<Site> getClosestNeutralSite(Sites allSites, Unit myQueen) {
        List<Site> neutralSites = allSites.values().stream().filter(Site::isNotConstructed).collect(Collectors.toList());
        if (neutralSites != null && !neutralSites.isEmpty()) {
            neutralSites.sort(
                    Comparator.comparing(site -> site.distanceToUnit(myQueen))
            );
            if (neutralSites.get(0) != null) {
                return Optional.of(neutralSites.get(0));
            }
        }
        return Optional.empty();
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

class Site extends Possession {
    int siteId;
    int abscisse;
    int ordonnee;
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

    public boolean isMine() {
        return structureType == 0;
    }

    public Double distanceToUnit(Unit unit) {
        return Math.sqrt(
                Math.pow(unit.abscisse - abscisse, 2) + Math.pow(unit.ordonnee - ordonnee, 2)
        );
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
        nbMines = sites.stream().filter(Site::isMine).count();
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
}

class Sites extends HashMap<Integer, Site> {}

class Units extends ArrayList<Unit> {
    Units(int numUnits) {
        super(numUnits);
    }
}

class Unit extends Possession {
    int abscisse;
    int ordonnee;
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
    String train(int[] ids) {
        StringBuilder base = new StringBuilder(CommandType.TRAIN.toString());
        for (int id : ids) {
            if (id > 0) {
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

    String moveTowardSite(Unit myqueen, Site site) {
        return CommandType.MOVE + Player.SPACE + site.abscisse + Player.SPACE + site.ordonnee;
    }
}

enum CommandType {
    WAIT,
    MOVE,
    BUILD,
    TRAIN;
}

enum UnitsType {
    KNIGHT(Player.KNIGHT, () -> SiteType.BARRACKS + "-" + Player.KNIGHT),
    ARCHER(Player.ARCHER, () -> SiteType.BARRACKS + "-" + Player.ARCHER),
    GIANT(Player.GIANT, () -> SiteType.BARRACKS + "-" + Player.GIANT);
    String name;
    Supplier<String> mappingToSite;

    UnitsType(String name, Supplier<String> mappingToSite) {
        this.name = name;
        this.mappingToSite = mappingToSite;
    }
}

enum SiteType {
    TOWER,
    BARRACKS;
}