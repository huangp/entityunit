package org.huangp.scanner;

import org.huangp.internal.BootstrapService;
import org.junit.Before;
import org.junit.Test;
import org.zanata.model.HTextFlowTarget;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class EntityClassScannerTest
{
   private EntityClassScanner scanner;

   @Before
   public void setUp() throws Exception
   {
      scanner = new EntityClassScanner();
   }

   @Test
   public void testScan() throws Exception
   {
      scanner.scan(HTextFlowTarget.class);

      System.out.println(BootstrapService.INSTANCE.print(HTextFlowTarget.class.getName()));
      System.out.println(BootstrapService.INSTANCE.print(HTextFlowTarget.class.getName()));
   }
}
