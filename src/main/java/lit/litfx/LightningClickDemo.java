package lit.litfx;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 *
 * @author phillsm1
 */
public class LightningClickDemo extends Application {
    
    @Override
    public void start(Stage primaryStage) {
//        StackPane root = new StackPane();
        BorderPane root = new BorderPane();
        root.setOnMouseClicked((MouseEvent event) -> {
            root.getChildren().clear();
            Polyline poly = null;
            if(event.getButton() == MouseButton.PRIMARY) {
                poly = Algorithms.toPolyLine(
                        Algorithms.simpleBres2D(
                            new Double(root.getWidth() / 2.0).intValue(), 
                            new Double(root.getHeight() / 2.0).intValue(),
                            new Double(event.getX()).intValue(), 
                            new Double(event.getY()).intValue()
                        )
                    );
                poly.setStroke(Color.STEELBLUE);
            } else if(event.getButton() == MouseButton.SECONDARY) {
                poly = new Bolt(
                    new Point2D(root.getWidth() / 2.0, root.getHeight() / 2.0), 
                    new Point2D(event.getX(), event.getY()), 
                    0.05, 80, 10, 0.75, 0.25);
                poly.setStroke(Color.ALICEBLUE);
            }
            if(null != poly) {
                root.getChildren().add(poly); 
                
                if(poly instanceof Bolt && event.isShiftDown()) {
                    ((Bolt)poly).setVisibleLength(0);
                    ((Bolt)poly).animate(Duration.millis(100));
                }
            }
            
        });

        Scene scene = new Scene(root, 300, 300, Color.BLACK);
        
        primaryStage.setTitle("Hello World!");
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
