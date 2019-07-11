package lit.litfx;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 *
 * @author phillsm1
 */
public class BranchLightningDemo extends Application {
    private Scene scene;
    
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("BranchLightningDemo.fxml"));
//        loader.setLocation(getClass().getResource("BranchLightningDemo.fxml"));
//        BorderPane borderPane = (BorderPane) loader.load();
        Parent parent = loader.load();
        scene = new Scene(parent, 800, 800, Color.BLACK);
        
        primaryStage.setTitle("FX Branch Lightning!");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}