package com.stefank3.quality.tests.live;

import com.stefank3.quality.components.ProductCard;
import com.stefank3.quality.pages.ProductsPage;
import com.stefank3.quality.tests.BaseTest;
import io.qameta.allure.Allure;
import java.math.BigDecimal;
import java.util.Locale;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Three catalogue scenarios based on observed, bounded public-site behavior. */
public final class ProductCatalogueTest extends BaseTest {
  /** Checks every displayed result against the verified known-name search contract. */
  @Test
  public void knownProductSearch() {
    ProductsPage products =
        Allure.step("Open catalogue", () -> new ProductsPage(driver, config).open());
    Allure.step(
        "Search for Blue Top",
        () -> {
          products.search("Blue Top");
        });
    var cards = products.cards();
    Assert.assertFalse(cards.isEmpty(), "Known product search must return results");
    Assert.assertTrue(
        products.heading().equalsIgnoreCase("Searched Products"),
        "Known-product search must display the Searched Products heading");
    for (ProductCard card : cards) {
      Assert.assertTrue(
          card.name().toLowerCase(Locale.ROOT).contains("blue top"),
          "Every displayed result must match the known-name query");
    }
  }

  /** Verifies the actual fields consumed by catalogue and cart workflows. */
  @Test
  public void productDetails() {
    var card = new ProductsPage(driver, config).open().productNamed("Blue Top");
    String id = card.id();
    String name = card.name();
    BigDecimal price = card.price();
    var details = Allure.step("Navigate from catalogue to product details", card::openDetails);
    Assert.assertEquals(
        details.id(), id, "Details must retain the chosen catalogue product identity");
    Assert.assertEquals(
        details.name(), name, "Details must retain the chosen catalogue product name");
    Assert.assertEquals(details.category(), "Women > Tops", "Blue Top must belong to Women > Tops");
    Assert.assertTrue(price.signum() > 0, "Catalogue price must be positive; actual=" + price);
    BigDecimal detailPrice = details.price();
    Assert.assertEquals(
        detailPrice.compareTo(price),
        0,
        "Detail price must match catalogue price; catalogue=" + price + ", detail=" + detailPrice);
    Assert.assertEquals(details.availability(), "In Stock", "Blue Top must be available in stock");
  }

  /** Supplies one negative and one boundary input; each row gets a fresh browser. */
  @DataProvider(parallel = false)
  public Object[][] searchCases() {
    return new Object[][] {{"qa-no-match-20261005", false}, {"", true}};
  }

  /**
   * Empty input restores the catalogue; an unmatched synthetic term renders an empty result set.
   */
  @Test(dataProvider = "searchCases")
  public void searchBoundary(String query, boolean restoresCatalogue) {
    ProductsPage products = new ProductsPage(driver, config).open();
    var originalIds = products.cards().stream().map(ProductCard::id).toList();
    Assert.assertFalse(originalIds.isEmpty(), "Catalogue must load before the boundary check");
    Allure.step(
        "Submit safe search input",
        () -> {
          products.search(query);
        });
    if (restoresCatalogue) {
      Assert.assertTrue(
          products.heading().equalsIgnoreCase("All Products"),
          "Empty search must restore the All Products heading");
      Assert.assertEquals(
          products.cards().stream().map(ProductCard::id).toList(),
          originalIds,
          "Empty search must restore the same catalogue product identities in the same order");
    } else {
      Assert.assertTrue(
          products.heading().equalsIgnoreCase("Searched Products"),
          "Unmatched search must display the Searched Products heading even with no results");
      Assert.assertTrue(products.cards().isEmpty(), "Unmatched query must have no displayed cards");
    }
  }
}
