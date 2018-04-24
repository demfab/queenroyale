import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by fabie on 23/04/2018.
 */
public class TestSites {

    @Test
    public void testFindClosestUpgradableTowerWhenQueenOrigin() {

        Site site1 = new Site(1, 1, 2, 3);
        site1.owner = 0;
        site1.structureType = 1;

        Site site2 = new Site(2, 2, 2, 3);
        site2.owner = 0;
        site1.structureType = 1;

        Site site3 = new Site(3, 2, 3 , 3);
        site3.owner = 1;
        site1.structureType = 1;

        Unit queen = new Unit(0, 0, 0, -1, 90);

        Sites sites = new Sites();
        sites.put(site1.siteId, site1);
        sites.put(site2.siteId, site2);
        sites.put(site3.siteId, site3);

        Site site = sites.findClosestUpgradableTower(queen).get();

        Assertions.assertEquals(1, site.siteId);
    }

    @Test
    public void testFindClosestUpgradableTowerWhenQueenX3Y3() {

        Site site1 = new Site(1, 1, 2, 3);
        site1.owner = 0;
        site1.structureType = 1;

        Site site2 = new Site(2, 2, 2, 3);
        site2.owner = 0;
        site2.structureType = 1;

        Site site3 = new Site(3, 2, 3 , 3);
        site3.owner = 0;
        site3.structureType = 1;

        Unit queen = new Unit(3, 3, 0, -1, 90);

        Sites sites = new Sites();
        sites.put(site1.siteId, site1);
        sites.put(site2.siteId, site2);
        sites.put(site3.siteId, site3);

        Site site = sites.findClosestUpgradableTower(queen).get();

        Assertions.assertEquals(3, site.siteId);
    }

    @Test
    public void testFindClosestUpgradableTowerWhenQueenX3Y3AndClosestTowerMaxHP() {

        Site site1 = new Site(1, 1, 2, 3);
        site1.owner = 0;
        site1.structureType = 1;
        site1.param1 = 100;

        Site site2 = new Site(2, 2, 2, 3);
        site2.owner = 0;
        site2.structureType = 1;
        site2.param1 = 200;

        Site site3 = new Site(3, 2, 3 , 3);
        site3.owner = 0;
        site3.structureType = 1;
        site3.param1 = Player.TOWER_MAX_HP;

        Unit queen = new Unit(3, 3, 0, -1, 90);

        Sites sites = new Sites();
        sites.put(site1.siteId, site1);
        sites.put(site2.siteId, site2);
        sites.put(site3.siteId, site3);

        Site site = sites.findClosestUpgradableTower(queen).get();

        Assertions.assertEquals(2, site.siteId);
    }
}
