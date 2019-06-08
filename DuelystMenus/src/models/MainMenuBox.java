package models;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.FileNotFoundException;

public class MainMenuBox extends VBox {
    private static final String DUELYST_LOGO_URL = "resources/ui/brand_duelyst.png";

    public MainMenuBox() throws FileNotFoundException {
        super(Constants.DEFAULT_SPACING * 5);
        ImageView brandView = ImageLoader.loadImage(
                DUELYST_LOGO_URL, Constants.DUELYST_LOGO_WIDTH, Constants.DUELYST_LOGO_HEIGHT
        );
        MainMenuGrid menuGrid = new MainMenuGrid();

        getChildren().addAll(brandView, menuGrid);
        setAlignment(Pos.CENTER);
        relocate(Constants.MAIN_MENU_BOX_X, Constants.MAIN_MENU_BOX_Y);
        setPadding(new Insets(Constants.DEFAULT_SPACING * 6));
    }
}