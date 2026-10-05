package com.stefank3.quality.tests.live;

import com.stefank3.quality.pages.ProductsPage;
import com.stefank3.quality.tests.BaseTest;
import io.qameta.allure.Allure;
import java.math.BigDecimal;
import org.testng.Assert;
import org.testng.annotations.Test;

/** One reversible anonymous cart scenario, intentionally ending before checkout. */
public final class CartTest extends BaseTest {
  /**
   * Verifies identity, price, quantity and arithmetic for exactly one product in a fresh session.
   */
  @Test
  public void addOneProductToCart() {
    var product = new ProductsPage(driver, config).open().productNamed("Blue Top");
    String id = product.id();
    String name = product.name();
    BigDecimal price = product.price();
    var details = Allure.step("Open chosen product", product::openDetails);
    var cart = Allure.step("Add one product and open cart", details::addOneToCart);
    Assert.assertEquals(cart.itemCount(), 1);
    Assert.assertEquals(cart.productId(id), id);
    Assert.assertEquals(cart.name(id), name);
    Assert.assertEquals(cart.unitPrice(id).compareTo(price), 0);
    Assert.assertEquals(cart.quantity(id), 1);
    Assert.assertEquals(
        cart.total(id).compareTo(price.multiply(BigDecimal.valueOf(cart.quantity(id)))), 0);
  }
}
