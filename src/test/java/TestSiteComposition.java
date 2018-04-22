import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by fabie on 22/04/2018.
 */
public class TestSiteComposition {

    @Test
    public void testSiteComposition () {
        Site site1 = new Site(1, 1, 2, 3);
        site1.owner = 0;

        Site site2 = new Site(2, 2, 2, 3);
        site2.owner = 0;

        Site site3 = new Site(3, 2, 3 , 3);
        site3.owner = 1;

        Sites sites = new Sites();
        sites.put(site1.siteId, site1);
        sites.put(site2.siteId, site2);
        sites.put(site3.siteId, site3);

        SiteComposition sitecomp = new SiteComposition(sites, Site::isMine);

        Assertions.assertEquals(2, sitecomp.sites.size());
    }

}
