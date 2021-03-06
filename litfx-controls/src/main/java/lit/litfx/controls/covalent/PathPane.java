package lit.litfx.controls.covalent;

import javafx.animation.*;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import lit.litfx.controls.covalent.CursorMappings.RESIZE_DIRECTION;
import lit.litfx.controls.covalent.events.CovalentPaneEvent;
import lit.litfx.core.components.StyleableRectangle;

import java.util.*;

import static lit.litfx.controls.covalent.BindablePointBuilder.*;
import static lit.litfx.controls.covalent.CursorMappings.RESIZE_DIRECTION.NONE;
import static lit.litfx.controls.covalent.CursorMappings.cursorMap;

public class PathPane extends AnchorPane {

    public SimpleStringProperty mainTitleTextProperty = new SimpleStringProperty("");
    public SimpleStringProperty mainTitleText2Property = new SimpleStringProperty("");
    private Scene scene;
    private Pane desktopPane;

    private double borderTimeMs, contentTimeMs;
    private ResizePaneTracker resizePaneTracker;
    private Map<Integer, Line> lineSegmentMap = new HashMap<>();
    private IntegerProperty segmentSelected = new SimpleIntegerProperty(-1);
    private RESIZE_DIRECTION[] cursorSegmentArray = new RESIZE_DIRECTION[14];


    public Pane contentPane;
    public Path outerFrame = null; //new Path();
    public MainContentViewArea mainContentBorderFrame;
    public Node leftAccent;
    public Node leftTab;
    public Pane windowButtons;
    public Pane mainTitleArea;
    private boolean enableDrag = true;
    // used for setupMovePaneSupport()
    private Point2D anchorPt;
    private Point2D previousLocation;
    private double contentAnchorGap = 15.0;
    private double mainContentViewRightAnchorGap = 15.0;
    Animation enterScene;
    SimpleBooleanProperty minimizedProperty = new SimpleBooleanProperty(false);

    public PathPane(Scene scene,
                    Pane desktopPane,
                    int width,
                    int height,
                    Pane userContent,
                    String mainTitleText,
                    String mainTitleText2,
                    double borderTimeMs,
                    double contentTimeMs) {
        this.scene = scene;
        this.desktopPane = desktopPane;
        this.contentPane = userContent;
        this.borderTimeMs = borderTimeMs;
        this.contentTimeMs = contentTimeMs;
        mainTitleTextProperty.set(null != mainTitleText ? mainTitleText : "");
        mainTitleText2Property.set(null != mainTitleText2 ? mainTitleText2 : "");
        setWidth(width);
        setHeight(height);
        getStyleClass().add("path-window-background");

        outerFrame = createFramePath(this);
        outerFrame.getStyleClass().add("outer-path-frame");

        // As the mouse hovers inside this PathPane determine what cursor to display.
        setOnMouseMoved(me -> {
            // update segment listener (s0 s2, s2, none...)
            // when segment listener's invalidation occurs fire cursor to change.
            logLineSegment(me.getX(), me.getY());
        });

        // reset cursor
        setOnMouseExited( mouseEvent -> {
            segmentSelected.set(-1);
            //System.out.println("mouse exited group");
        });
//        scene.setOnMouseMoved(me -> {
//            // update segment listener (s0 s2, s2, none...)
//            // when segment listener's invalidation occurs fire cursor to change.
//            logLineSegment(me.getSceneX(), me.getSceneY());
//            outerFrame.toBack();
////            System.out.println("scene mouse moved");
//        });
//
//        // reset cursor
//        scene.setOnMouseExited( mouseEvent -> {
//            segmentSelected.set(-1);
//            System.out.println("mouse exited group");
//        });

        // createWindowButtons this is the title area and three buttons on top left.
        windowButtons = createWindowButtons(this);
        setupMovePaneSupport(windowButtons);

        // TODO initialize if windows are staggered on the desktop area.
        anchorPt = new Point2D(0,0);
        previousLocation = new Point2D(0,0);

        // createLeftAccent
        leftAccent = createLeftAccent(this);
        setupMovePaneSupport(leftAccent);

        // createLeftTab
        leftTab = createLeftTab(this);
        setupMovePaneSupport(leftTab);

        // createMainTitleArea
        mainTitleArea = createMainTitleArea();
        setupMovePaneSupport(mainTitleArea);

        // build bottom area
        mainContentBorderFrame = createMainContentViewArea();

        // IMPORTANT THIS IS THE CONTENT SET INTO THE main content pane (nestedPane)
        AnchorPane.setTopAnchor(contentPane, contentAnchorGap);
        AnchorPane.setLeftAnchor(contentPane, contentAnchorGap);
        AnchorPane.setRightAnchor(contentPane, contentAnchorGap);
        AnchorPane.setBottomAnchor(contentPane, contentAnchorGap);
        mainContentBorderFrame.getMainContentPane().getChildren().add(this.contentPane);

        //Disable interactions with the content while minimized.
        contentPane.mouseTransparentProperty().bind(minimizedProperty);
        
        getChildren().addAll(outerFrame, windowButtons, mainTitleArea,
            leftAccent, leftTab,  mainContentBorderFrame);
        enterScene = createEnterAnimation(
                this.contentPane,
                windowButtons,
                mainTitleArea,
                leftAccent,
                leftTab,
                outerFrame,
                mainContentBorderFrame.getMainContentInnerPath());
        

        //if the pane is minimized, double click to bring it back
        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if(minimizedProperty.get() && 
                e.getClickCount() > 1 && e.getButton() == MouseButton.PRIMARY) {
                restore();
            }
        });
        //if the pane is minimized, enable all components to handle mouse presses
//        addEventHandler(MouseEvent.MOUSE_PRESSED, me -> {
//            if(minimizedProperty.get()) {
//                handleMousePressed(me);
//                me.consume();
//            }
//        });
        //if the pane is minimized, enable all components to assist with dragging
//        addEventHandler(MouseEvent.MOUSE_DRAGGED, me -> {
//            if(minimizedProperty.get()) {
//                handleMouseDragged(me);
//                me.consume();
//            }
//        });
                
        // Initialize previousLocation after Stage is shown
        //@TODO SMP Replace Stage oriented Window events with custom versions
        this.scene.addEventHandler(CovalentPaneEvent.COVALENT_PANE_SHOWN,
            (CovalentPaneEvent t) -> {
                previousLocation = new Point2D(getTranslateX(),getTranslateY());
            });


        wireListeners();
    }

    /**
     * Position window mouse pressed
     * @param mouseEvent
     */
    private void handlePositionWindowMousePressed(MouseEvent mouseEvent) {
        anchorPt = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());
    }

    /**
     * Position window mouse dragged
     * @param mouseEvent
     */
    private void handlePositionWindowMouseDragged(MouseEvent mouseEvent) {
        if(isEnableDrag()) {        
            if (anchorPt != null && previousLocation != null) {
                this.setTranslateX(previousLocation.getX()
                        + mouseEvent.getSceneX()
                        - anchorPt.getX());
                this.setTranslateY(previousLocation.getY()
                        + mouseEvent.getSceneY()
                        - anchorPt.getY());
            }
        }
    }

    /**
     * Positioning window mouse release
     * @param mouseEvent
     */
    private void handlePositionWindowMouseReleased(MouseEvent mouseEvent) {
        previousLocation = new Point2D(getTranslateX(),getTranslateY());
    }

    /**
     * This convient function allows any node to be allow user to drag or position
     * window in the scene. For example the title bar or left accent allows the
     * user to move the pane(window).
     *
     * @param node
     */
    private void setupMovePaneSupport(Node node){
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handlePositionWindowMousePressed);
        node.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handlePositionWindowMouseDragged);
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handlePositionWindowMouseReleased);
    }

    private Animation createEnterBorderAnimation(Path borderFrame, double totalMS) {
        double totalLength = Utils.getTotalLength(borderFrame);
        borderFrame.getStrokeDashArray().add(totalLength);
        borderFrame.setStrokeDashOffset(totalLength);
        KeyValue strokeOffsetStart = new KeyValue(borderFrame.strokeDashOffsetProperty(), totalLength);
        KeyValue visible = new KeyValue(borderFrame.visibleProperty(), true);
        KeyFrame keyFrame1 = new KeyFrame(Duration.millis(1), strokeOffsetStart, visible);
        KeyValue strokeOffsetEnd = new KeyValue(borderFrame.strokeDashOffsetProperty(), 0);
        KeyFrame keyFrame2 = new KeyFrame(Duration.millis(300), handler -> {
            System.out.println("done.");
            borderFrame.getStrokeDashArray().clear();
        }, strokeOffsetEnd);

        Timeline anim = new Timeline(keyFrame1, keyFrame2);
        return anim;
    }
    private Animation createEnterAnimation(Pane pane,
                                        Pane windowButtons,
                                        Pane mainTitleArea,
                                        Node leftAccent,
                                        Node leftTab,
                                        Path outerBorderFrame,
                                        Path mainBorderFrame) {

        ParallelTransition borderParallelTransition = new ParallelTransition();
        Animation anim1 = createEnterBorderAnimation(outerBorderFrame, borderTimeMs);
        Animation anim2 = createEnterBorderAnimation(mainBorderFrame, borderTimeMs);
        borderParallelTransition.getChildren().addAll(anim1, anim2);

        ParallelTransition windowParallelTransition = new ParallelTransition();
        Animation anim3 = createFadeAnim(windowButtons, contentTimeMs);
        Animation anim4 = createFadeAnim(mainTitleArea, contentTimeMs);
        Animation anim5 = createFadeAnim(leftAccent, contentTimeMs);
        Animation anim6 = createFadeAnim(leftTab, contentTimeMs);
        windowParallelTransition.getChildren().addAll(anim3, anim4, anim5, anim6);
        
        Animation anim7 = createFadeAnim(pane, contentTimeMs); //createEnterRootAnim(1000, stage);
        SequentialTransition sequentialTransition = new SequentialTransition();
        sequentialTransition
                .getChildren()
                .addAll(
                    anim7,
                    borderParallelTransition,
                    windowParallelTransition);                    
//                    anim7); //show content last
//                    anim3, anim4, anim5, anim6);
        return sequentialTransition;
    }

    private Animation createFadeAnim(Node view, double totalTimeMs) {
        FadeTransition fadeTransition = new FadeTransition();
        fadeTransition.setNode(view);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.setDuration(Duration.millis(totalTimeMs));
        return fadeTransition;
    }

    private Node createLeftAccent(AnchorPane root) {
        // create the accent shape left
        ShapedPath accentShape = ShapedPathBuilder.create(root)
                .addStyleClass("window-accent-shape")
                .moveTo(20, 9)
                .horzSeg(35)
                .vertSeg(9+15)
                .lineSeg(15,9+15+20)
                .vertSeg(9+15)
                .lineSeg(20,9+15)
                .closeSeg()
                .build();

        //root.getChildren().add(accentShape);
        return accentShape;
    }

    private Node createLeftTab(AnchorPane root) {
        ShapedPath leftTabShape = ShapedPathBuilder.create(root)
                .addStyleClass("left-tab-shape")
                .moveTo(5, 30+3)
                .lineSeg(xTo(13).yTo(9+15))
                .vertSeg(yTo(9+140-13))
                .lineSeg(xTo(5).yTo(9+140-13-8))
                .closeSeg()
                .build();

        return leftTabShape;
    }
    private Pane createWindowButtons(AnchorPane root) {

        AnchorPane  windowHeader = new AnchorPane();
        windowHeader.getStyleClass().add("window-header");

        windowHeader.setPrefHeight(12);
        AnchorPane.setTopAnchor(windowHeader, 8.0);

        double leftAnchor = 10 + 10 + 15;
        AnchorPane.setLeftAnchor(windowHeader, leftAnchor);

        double rightAnchor = 15;
        AnchorPane.setRightAnchor(windowHeader, rightAnchor);

        // create buttons
        HBox buttonArea = new HBox(4);
        StyleableRectangle b1 = new StyleableRectangle(50, 20, Color.rgb(253,253,253, .8));
        b1.getStyleClass().add("window-header-minimize-button");
        b1.setOnMouseClicked(e -> {
            b1.getScene().getRoot().fireEvent(
                new CovalentPaneEvent(CovalentPaneEvent.COVALENT_PANE_MINIMIZE, this));
            this.minimize();
        });
        
        StyleableRectangle b2 = new StyleableRectangle(50, 20, Color.rgb(235,235,80, .8));
        b2.getStyleClass().add("window-header-maximize-button");
        b2.setOnMouseClicked(e ->{
            b2.getScene().getRoot().fireEvent(
                new CovalentPaneEvent(CovalentPaneEvent.COVALENT_PANE_MAXIMIZE, this));
            this.maximize();
        });

        StyleableRectangle b3 = new StyleableRectangle(50, 20, Color.rgb(250, 50, 50, .8));
        b3.getStyleClass().add("window-header-close-button");
        b3.setOnMouseClicked(e -> {
            b3.getScene().getRoot().fireEvent(
                new CovalentPaneEvent(CovalentPaneEvent.COVALENT_PANE_CLOSE, this));
            this.close();
        });
        
        buttonArea.getChildren().addAll(b1,b2, b3);

        AnchorPane.setTopAnchor(buttonArea, (double)(12-5f)/2);
        double buttonAreaRightAnchor = 5;
        AnchorPane.setRightAnchor(buttonArea, buttonAreaRightAnchor);

        windowHeader.getChildren().add(buttonArea);

        return windowHeader;
    }

    private Pane createMainTitleArea() {

        AnchorPane mainTitleView = new AnchorPane();
        mainTitleView.setPrefHeight(70);
        AnchorPane.setTopAnchor(mainTitleView, 30.0);

        double leftAnchor = 10 + 15;
        AnchorPane.setLeftAnchor(mainTitleView, leftAnchor);

        double rightAnchor = 15;
        AnchorPane.setRightAnchor(mainTitleView, rightAnchor);

        Path titlePath = new Path();
        titlePath.getStyleClass().add("main-title-path");
        MoveTo a0 = new MoveTo(10,5);
        LineTo a1 = new LineTo();
        a1.xProperty().bind(mainTitleView.widthProperty());
//        a1.yProperty().set(10);
        a1.yProperty().set(5);

        LineTo a2 = new LineTo();
        a2.xProperty().bind(mainTitleView.widthProperty());
        a2.yProperty().bind(mainTitleView.heightProperty().subtract(10 + 5));

//        LineTo a3 = new LineTo();
//        a3.xProperty().bind(mainTitleView.widthProperty().subtract(10));
//        a3.yProperty().bind(mainTitleView.heightProperty().subtract(10 + 10));

        LineTo a4 = new LineTo();
        a4.xProperty().set(10);
        a4.yProperty().bind(mainTitleView.heightProperty().subtract(10 + 5));

        LineTo a5 = new LineTo();
        a5.xProperty().set(0);
        a5.yProperty().bind(mainTitleView.heightProperty().subtract(7));

        LineTo a6 = new LineTo();
        a6.xProperty().set(0);
        a6.yProperty().set(15);

        ClosePath closePath = new ClosePath();
        titlePath.getElements()
                .addAll(a0, a1, a2, /*a3,*/ a4, a5, a6, closePath);


        AnchorPane nestedPane = new AnchorPane(); // has the clipped
        Path titlePathAsClip = new Path();
        titlePathAsClip.getElements().addAll(a0, a1, a2, /*a3,*/ a4, a5, a6, closePath);
        titlePathAsClip.setFill(Color.WHITE);
        nestedPane.setClip(titlePathAsClip);

        Text text1 = new Text(mainTitleTextProperty.get() + " ");
        text1.textProperty().bind(mainTitleTextProperty);
        text1.setFill(Color.WHITE);
        text1.getStyleClass().add("main-title-text");

        Text text2 = new Text(mainTitleText2Property.get());
        text2.textProperty().bind(mainTitleText2Property);
        text2.setFill(Color.WHITE);
        text2.getStyleClass().add("main-title-text2");

        TextFlow textFlow = new TextFlow(text1, text2);
        AnchorPane.setLeftAnchor(textFlow, 20.0);
        AnchorPane.setTopAnchor(textFlow, 5.0);
        nestedPane.getChildren().add(textFlow);

        mainTitleView.getChildren().addAll(titlePath, nestedPane);

        return mainTitleView;

    }

    private MainContentViewArea createMainContentViewArea() {
        // Create a main content region
        // 1) create pane (transparent style)
        // 2) create path outline
        // 3) use path outline as clip region
        MainContentViewArea mainContentView = new MainContentViewArea();
        mainContentView.getStyleClass().add("main-content-view");
        AnchorPane.setTopAnchor(mainContentView, 90.0);
        double leftAnchor = 10 + 15;
        AnchorPane.setLeftAnchor(mainContentView, leftAnchor);
        double rightAnchor = 15;
        AnchorPane.setRightAnchor(mainContentView, rightAnchor);
        AnchorPane.setBottomAnchor(mainContentView, 10 + 15.0);
        //root.getChildren().add(mainContentView);

        ShapedPath mainContentInnerPath = ShapedPathBuilder.create(mainContentView)
                .addStyleClass("main-content-inner-path")
                .moveTo(10, 0)
                .horzSeg(bindXToWidth(-10))  // 0
                .lineSeg(bindXToWidth().yTo(10))   // 1
                .vertSeg(bindYToHeight(-50)) // 2
                .lineSeg(bindXToWidth(-10).bindYToHeight(- (50 - 10))) //3
                .horzSeg(bindXToWidth(-(190-10))) // 4
                .lineSeg(bindXToPrevX(-10).bindYToHeight(-(50-10-10))) //5
                .vertSeg(bindXToPrevX(0).bindYToHeight(-10)) // 6
                .lineSeg(bindXToPrevX(-10).bindYToHeight()) // 7
                .horzSeg(xTo(10))
                .lineSeg(xTo(0).bindYToHeight(-10))
                .vertSeg(10)
                .closeSeg()
                .build();

        // THIS IS WHERE THE USER CONTENT IS PLACED INTO!!!!
        AnchorPane nestedPane = new AnchorPane(); // has the clipped
        nestedPane.getStyleClass().add("main-content-pane");
        // clone path for clip region
        Path mainContentInnerPathAsClip = new Path();
        mainContentInnerPathAsClip.getElements().addAll(mainContentInnerPath.getElements());
        mainContentInnerPathAsClip.setFill(Color.WHITE);

        // punch out the path for the main content
        nestedPane.setClip(mainContentInnerPathAsClip);

        // set the nested pane for the caller to put stuff inside.
        mainContentView.setMainContentPane(nestedPane);
        //Add the nestedPane containging user content last so it has mouse priority
        mainContentView.getChildren().addAll(mainContentInnerPath, nestedPane);

        mainContentView.setMainContentInnerPath(mainContentInnerPath);

        return mainContentView;
    }
    
    private Animation createMinimizeAnim(double totalTimeMs) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(totalTimeMs), this);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(0.10);
        scaleTransition.setToY(0.10);
        return scaleTransition;
    }
    private Animation createRestoreAnim(double totalTimeMs) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(totalTimeMs), this);
        scaleTransition.setFromX(0.1);
        scaleTransition.setFromY(0.1);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        return scaleTransition;
    }    
    public void minimize() {
        System.out.println("Minimize called on Pane: " + this.toString());
        Animation minimizeAnimation = createMinimizeAnim(contentTimeMs);
        minimizedProperty.set(true);
        minimizeAnimation.play();
    }
    public void restore() {
        System.out.println("Restoring Pane: " + this.toString());
        Animation restoreAnimation = createRestoreAnim(contentTimeMs);
        minimizedProperty.set(false);
        restoreAnimation.play();
    }
    public void maximize() {
        System.out.println("Maximize called on Pane: " + this.toString());
        
    }
    public void close() {
        System.out.println("Close called on Pane: " + this.toString());
    }
    public void show() {
        enterScene.play();
    }

    private RESIZE_DIRECTION getCurrentResizeDirection() {
        int segmentIndex = segmentSelected.get();
        if (segmentIndex == -1) return NONE;
        return cursorSegmentArray[segmentIndex];
    }
    private void wireListeners() {

        // Rework the resize pane tracker work...
        resizePaneTracker = new ResizePaneTracker(this, desktopPane);

        resizePaneTracker.setOnMousePressed((mouseEvent, wt) -> {

            Point2D windowXY = new Point2D(getTranslateX(), getTranslateY());
            wt.anchorPathPaneXYCoordValue.set(windowXY);

            // TODO Revisit code b/c this might be doing the same thing as line above.
            wt.paneXCoordValue.set(windowXY.getX());
            wt.paneYCoordValue.set(windowXY.getY());

            // anchor of the mouse screen x,y position.
            // store anchor x,y of the PathPane parent (upper left)
            Point2D mouseDesktopXY = new Point2D(mouseEvent.getX(), mouseEvent.getY());
            wt.anchorCoordValue.set(mouseDesktopXY);

            // current width and height
            wt.anchorWidthSizeValue.set(getWidth());
            wt.anchorHeightSizeValue.set(getHeight());
            System.out.println("press mouseX = " + mouseEvent.getX() + " translateX = " + getTranslateX());

            // current resize direction
            wt.currentResizeDirection.set(getCurrentResizeDirection());

            // current line segment
            wt.currentSegmentIndex.set(segmentSelected.get());
        });

        resizePaneTracker.setOnMouseDragged((mouseEvent, wt) -> {

            RESIZE_DIRECTION direction = wt.currentResizeDirection.get();

            switch (direction) {
                case NW:
                    // TODO Northwest or Upper Left accuracy
                    resizeNorth(mouseEvent, wt);
                    resizeWest(mouseEvent, wt);
                    break;
                case N:
                    resizeNorth(mouseEvent, wt);
                    break;
                case NE:
                    //TODO Northeast Upper right corner accuracy
                    resizeNorth(mouseEvent, wt);
                    resizeEast(mouseEvent, wt);
                    break;
                case E:
                    resizeEast(mouseEvent, wt);
                    break;
                case SE:
                    resizeSouth(mouseEvent, wt);
                    resizeEast(mouseEvent, wt);
                    break;
                case S:
                    resizeSouth(mouseEvent, wt);
                    break;
                case SW:
                    resizeSouth(mouseEvent, wt);
                    resizeWest(mouseEvent, wt);
                    break;
                case W:
                    // TODO update offset West left side accuracy
                    resizeWest(mouseEvent, wt);
                    break;
                default:
                    break;
            }

        });

// // Rework the resize pane tracker work...
//        resizePaneTracker = new ResizePaneTracker(contentPane);
//
//        resizePaneTracker.setOnMousePressed((mouseEvent, wt) -> {
//            // store anchor x,y of the stage
//            wt.anchorStageXYCoordValue.set(new Point2D(getTranslateX(), getTranslateY()));
//
//            // TODO Revisit code b/c this might be doing the same thing as line above.
//            wt.paneXCoordValue.set(getTranslateX());
//            wt.paneYCoordValue.set(getTranslateY());
//
//            // anchor of the mouse screen x,y position.
//            wt.anchorCoordValue.set(new Point2D(mouseEvent.getScreenX(), mouseEvent.getScreenY()));
//
//            // current width and height
//            wt.anchorWidthSizeValue.set(contentPane.getWidth());
//            wt.anchorHeightSizeValue.set(contentPane.getHeight());
//            System.out.println("press mouseX = " + mouseEvent.getX() + " translateX = " + getTranslateX());
//
//            // current resize direction
//            wt.currentResizeDirection.set(getCurrentResizeDirection());
//
//            // current line segment
//            wt.currentSegmentIndex.set(segmentSelected.get());
//        });
//
//        resizePaneTracker.setOnMouseDragged((mouseEvent, wt) -> {
//
////        resizePaneTracker.setOnMouseDragged((mouseEvent, wt) -> {
//            RESIZE_DIRECTION direction = wt.currentResizeDirection.get();
//
//            switch (direction) {
//                case NW:
//                    // TODO Northwest or Upper Left accuracy
//                    resizeNorth(mouseEvent, wt);
//                    resizeWest(mouseEvent, wt);
//                    break;
//                case N:
//                    resizeNorth(mouseEvent, wt);
//                    break;
//                case NE:
//                    //TODO Northeast Upper right corner accuracy
//                    resizeNorth(mouseEvent, wt);
//                    resizeEast(mouseEvent, wt);
//                    break;
//                case E:
//                    resizeEast(mouseEvent, wt);
//                    break;
//                case SE:
//                    resizeSouth(mouseEvent, wt);
//                    resizeEast(mouseEvent, wt);
//                    break;
//                case S:
//                    resizeSouth(mouseEvent, wt);
//                    break;
//                case SW:
//                    resizeSouth(mouseEvent, wt);
//                    resizeWest(mouseEvent, wt);
//                    break;
//                case W:
//                    // TODO update offset West left side accuracy
//                    resizeWest(mouseEvent, wt);
//                    break;
//                default:
//                    break;
//            }
//
//        });

        // after user resizes (mouse release) the previous location is reset
        resizePaneTracker.setOnMouseReleased((mouseEvent, wt) -> {
            previousLocation = new Point2D(getTranslateX(),getTranslateY());
        });
                // Mouse cursor touching segments
        segmentSelected.addListener( (ob, oldv, newv) -> {
            int index = newv.intValue();
            if (index > -1) {
                RESIZE_DIRECTION direction = cursorSegmentArray[index];
                scene.cursorProperty().set(cursorMap.get(direction));
            } else {
                scene.cursorProperty().set(Cursor.DEFAULT);
            }
        });

        // populate the lines for the outerframe.
        //@TODO SMP Replace with custom event
        scene.getRoot().addEventHandler(CovalentPaneEvent.COVALENT_PANE_SHOWN, e -> {
            generateLineMap(outerFrame.getElements());
        });
        generateLineMap(outerFrame.getElements());
    }

    private void resizeNorth(MouseEvent mouseEvent, ResizePaneTracker wt) {
        // Note: mouse cursor x,y is local to this Pane and has to be local to parent screen coordinate.
        Point2D desktopPoint = localToParent(mouseEvent.getX(), mouseEvent.getY());
        double screenY = desktopPoint.getY();
        double distance = wt.anchorPathPaneXYCoordValue.get().getY() - screenY;

        wt.paneYCoordValue.set(wt.anchorPathPaneXYCoordValue.get().getY() - distance);
        double newHeight = wt.anchorHeightSizeValue.get() + distance;
        wt.resizeHeightValue.set(newHeight);
    }

    private void resizeSouth(MouseEvent mouseEvent, ResizePaneTracker wt) {
        double screenY = mouseEvent.getY();
        double newHeight = wt.anchorHeightSizeValue.get() + screenY - wt.anchorCoordValue.get().getY();
        wt.resizeHeightValue.set(newHeight);
    }

    private void resizeEast(MouseEvent mouseEvent, ResizePaneTracker wt) {
        double screenX = mouseEvent.getX();
        double newWidth = wt.anchorWidthSizeValue.get() + screenX - wt.anchorCoordValue.get().getX();
        wt.resizeWidthValue.set(newWidth);
    }

    private void resizeWest(MouseEvent mouseEvent, ResizePaneTracker wt) {
        //System.out.println("mouse x, y " + mouseEvent.getX() + ", " + mouseEvent.getY());
        // Note: mouse cursor x,y is local to this Pane and has to be local to parent screen coordinate.
        Point2D desktopPoint = localToParent(mouseEvent.getX(), mouseEvent.getY());

        double screenX = desktopPoint.getX();
        double offset = wt.currentSegmentIndex.intValue() == 8 ? 10 : 0; // TODO magic numbers fix.
        double distance = wt.anchorPathPaneXYCoordValue.get().getX() - screenX + offset; // offset left side segment 8 (10 pixels)
        wt.paneXCoordValue.set(wt.anchorPathPaneXYCoordValue.get().getX() - distance);

        double newWidth = wt.anchorWidthSizeValue.get() + distance;
        wt.resizeWidthValue.set(newWidth);
    }

    /**
     * As mouse move (hover) over this PathPane mouse x,y local to this Pane.
     * Iterates through all Line segments to determine if the cursor pointer is
     * near a segement. If so, set the cursor based on the resized directions.
     * @param targetX PathPane mouse cursor x position
     * @param targetY PathPane mouse cursor y position
     * @return Map.Entry<Integer, Line> The Map entry of segment number and Line object.
     */
    private Map.Entry<Integer, Line> logLineSegment(double targetX, double targetY) {
        Set<Map.Entry<Integer, Line>> entries = lineSegmentMap.entrySet();
        Optional<Map.Entry<Integer, Line>> entry = entries.stream()
                .filter(segNumLine ->
                  Utils.isPointNearLine(targetX, targetY,
                        segNumLine.getValue(),
                        5,
                        segNumLine.getKey()))
                .findAny();

        entry.ifPresentOrElse(
                a -> segmentSelected.set(a.getKey()), // set segment num selected
                () -> segmentSelected.set(-1));       // set -1 to default to mouse cursor

        return entry.isPresent() ? entry.get() : null;
    }

    private void generateLineMap(List<PathElement> framePath) {
        PathElement prev = null;
        int cnt = 0;

        for(PathElement pe:framePath) {
            if (prev == null) {
                prev = pe;
                continue;
            }
            if (pe instanceof ClosePath) {
                continue;
            }
            Line segment = createLine(prev, pe);

            //segment.startXProperty().bind(((MoveTo)prev).;
            lineSegmentMap.put(cnt, segment);

            Line line = segment;
//            double sX = line.startXProperty().get();
//            double sY = line.startYProperty().get();
//            double eX = line.endXProperty().get();
//            double eY = line.endYProperty().get();
//            System.out.print("segment name " + cnt + " line x1,y1 to x2,y2 ");
//            System.out.printf(" (%s, %s) (%s, %s) \n", sX, sY, eX, eY);
            prev = pe;
            cnt++;
        }
        Line segment = createLine(prev, framePath.get(0));
        lineSegmentMap.put(cnt, segment);
    }
    private <T> T getPathElementAs(Class<?> clazz, PathElement pathElement) {
        return (T) clazz.cast(pathElement);
    }

    private Line createLine(PathElement p1, PathElement p2) {
        Line line = null;
        if (p1 instanceof MoveTo) {
            MoveTo moveTo = getPathElementAs(MoveTo.class, p1);
            LineTo lineTo = getPathElementAs(LineTo.class, p2);
            line = new Line();

            line.startXProperty().bind(moveTo.xProperty());
            line.startYProperty().bind(moveTo.yProperty());
            line.endXProperty().bind(lineTo.xProperty());
            line.endYProperty().bind(lineTo.yProperty());
        } else if (p1 instanceof LineTo && p2 instanceof LineTo) {
            LineTo p1lineTo = getPathElementAs(LineTo.class, p1);
            LineTo p2lineTo = getPathElementAs(LineTo.class, p2);
            line = new Line();

            line.startXProperty().bind(p1lineTo.xProperty());
            line.startYProperty().bind(p1lineTo.yProperty());
            line.endXProperty().bind(p2lineTo.xProperty());
            line.endYProperty().bind(p2lineTo.yProperty());

        } else if (p1 instanceof LineTo && p2 instanceof MoveTo) {
            LineTo lineTo = getPathElementAs(LineTo.class, p1);
            MoveTo moveTo = getPathElementAs(MoveTo.class, p2);

            line = new Line();

            line.startXProperty().bind(lineTo.xProperty());
            line.startYProperty().bind(lineTo.yProperty());
            line.endXProperty().bind(moveTo.xProperty());
            line.endYProperty().bind(moveTo.yProperty());
        }
        return line;
    }

    /**
     * Creates the outer frame or line segments to be draggable to resize the window.
     * @param pane
     * @return
     */
    public Path createFramePath(Pane pane) {
        // draw
        ShapedPath newFrame = ShapedPathBuilder.create(pane)
                .moveTo(20, 0)
                .horzSeg(bindXToWidth(-10)) // 0
                .lineSeg(bindXToWidth().yTo(10))  // 1
                .vertSeg(bindYToHeight(-10)) // 2
                .lineSeg(bindXToWidth(-10).bindYToHeight()) //3
                .horzSeg(bindXToWidth(-200)) // 4
                .lineSeg(bindXToWidth(-210).bindYToHeight(-10)) // 5
                .horzSeg(xTo(20).bindYToHeight(-10)) // 6
                .lineSeg(xTo(10).bindYToHeight(-20))  //7
                .vertSeg(140) //8
                .lineSeg(0,130) //9
                .vertSeg(30) //10
                .lineSeg(10, 20) // 11
                .vertSeg(10)
                .closeSeg()
                .build();

        // Assign mouse cursor direction for each segment.
        // When the user hovers over each line segment
        // the mouse cursor is based on the 8 directions.
        cursorSegmentArray[0] = RESIZE_DIRECTION.N;
        cursorSegmentArray[1] = RESIZE_DIRECTION.NE;
        cursorSegmentArray[2] = RESIZE_DIRECTION.E;
        cursorSegmentArray[3] = RESIZE_DIRECTION.SE;
        cursorSegmentArray[4] = RESIZE_DIRECTION.S;
        cursorSegmentArray[5] = RESIZE_DIRECTION.S;
        cursorSegmentArray[6] = RESIZE_DIRECTION.S;
        cursorSegmentArray[7] = RESIZE_DIRECTION.SW;
        cursorSegmentArray[8] = RESIZE_DIRECTION.W;
        cursorSegmentArray[9] = RESIZE_DIRECTION.W;
        cursorSegmentArray[10] = RESIZE_DIRECTION.W;
        cursorSegmentArray[11] = RESIZE_DIRECTION.NW;
        cursorSegmentArray[12] = RESIZE_DIRECTION.NW;
        cursorSegmentArray[13] = RESIZE_DIRECTION.NW;

        return newFrame;
    }

    /**
     * @return the enableDrag
     */
    public boolean isEnableDrag() {
        return enableDrag;
}

    /**
     * @param enableDrag the enableDrag to set
     */
    public void setEnableDrag(boolean enableDrag) {
        this.enableDrag = enableDrag;
    }

    /**
     * @return the contentAnchorGap
     */
    public double getContentAnchorGap() {
        return contentAnchorGap;
    }

    /**
     * @param contentAnchorGap the contentAnchorGap to set
     */
    public void setContentAnchorGap(double contentAnchorGap) {
        this.contentAnchorGap = contentAnchorGap;
    }
}

interface PaneMousePressed {
    void pressed(MouseEvent mouseEvent, ResizePaneTracker paneTracker);
}
interface PaneMouseDragged {
    void dragged(MouseEvent mouseEvent, ResizePaneTracker paneTracker);
}
interface PaneMouseReleased {
    void released(MouseEvent mouseEvent, ResizePaneTracker paneTracker);
}
