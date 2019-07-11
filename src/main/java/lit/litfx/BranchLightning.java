package lit.litfx;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;

/**
 *
 * @author phillsm1
 * 
 */
public class BranchLightning extends Group {
    public Bolt primaryBolt;
    public ArrayList<Bolt> branchList;
    Color boltColor = Color.ALICEBLUE;
    Color branchColor = Color.STEELBLUE;
    double boltStrokeWidth = 5;
    double branchStrokeWidth = 2;
    int branches;
    double density;
    double sway;
    double jitter;
    double branchAngleLimit = 20.0;
    double envelopeSize = 0.75;
    double envelopeScaler = 0.1;
    SimpleIntegerProperty pointIndexProperty = new SimpleIntegerProperty(0);
    
    public enum Member {PRIMARYBOLT, BRANCH};
    
    public BranchLightning(Point2D start, Point2D end, 
        double density, double sway, double jitter,
        int branches, double branchAngleLimit) {

        this.density = density;
        this.sway = sway;
        this.jitter = jitter;
        this.branches = branches;
        this.branchAngleLimit = branchAngleLimit;
        
        //create the baseline bolt
        primaryBolt = new Bolt(start, end, density, sway, jitter, 0.75, 0.25);
        primaryBolt.setStroke(boltColor);
        primaryBolt.setStrokeWidth(boltStrokeWidth);
        getChildren().add(primaryBolt);
                
        //Determine randomly where the branches should be
        ArrayList<BoltPoint> branchPoints = randomBranchPoints(branches, primaryBolt);
        //For each branch location, generate a new bolt of lightning
        branchList = new ArrayList<>();
        Random random = new Random();
        for(int i=0; i<branchPoints.size(); i++) {
            //Get the starting location for the new branch bolt
            int startIndex = primaryBolt.getBoltPoints().indexOf(branchPoints.get(i));
            //Is it the last bolt point?
            if(startIndex >= primaryBolt.getBoltPoints().size()-1) {
                //back everything up by 1
                startIndex = startIndex - 1;
            } 
            BoltPoint startBoltPoint = primaryBolt.getBoltPoints().get(startIndex);
            //Use the actual end point of the original bolt as a "heading" 
            //This allows reasonable looking divergence of the branches
            BoltPoint endBoltPoint = new BoltPoint(1, end);
            //Calculate the actual angle of the line with regard to the screen
            //Screen coordinates is Y positive down, 0,0 upper left corner
            Point2D startPoint2D = new Point2D(startBoltPoint.getX(), startBoltPoint.getY());
            double baseAngle = Math.toDegrees(Math.atan2(
                endBoltPoint.getY()-startBoltPoint.getY(), 
                endBoltPoint.getX()-startBoltPoint.getX()));
            //add a random angle to diverge from the base bolt
            double deltaAngle = ThreadLocalRandom.current().nextDouble(-branchAngleLimit, branchAngleLimit);
            double finalAngleRadians = Math.toRadians(baseAngle + deltaAngle);
            //System.out.println("baseAngle: " + baseAngle 
            //    + " deltaAngle: " + deltaAngle + " finalAngleRadians: " + finalAngleRadians);
            
            //Figure out endpoint based on original bolt distance and angle from normal
            double branchLength = random.nextDouble() * startPoint2D.distance(end);
            Point2D endPoint2D = new Point2D(
                startPoint2D.getX() + branchLength * Math.cos(finalAngleRadians), 
                startPoint2D.getY() + branchLength * Math.sin(finalAngleRadians)
            );
            Bolt branch = new Bolt(startPoint2D, endPoint2D, density, sway, jitter, 0.9, 0.1);
            branch.setStroke(branchColor);
            branch.setStrokeWidth(branchStrokeWidth);
            branchList.add(branch);
        }

        getChildren().addAll(branchList);
    }
    
    private ArrayList<BoltPoint> randomBranchPoints(int branches, Bolt bolt) {
        // pick a bunch of random points on the Bolt
        ArrayList<Integer> positions = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < branches; i++) {
            positions.add(
                Math.round(random.nextFloat() * (bolt.getBoltPoints().size()-1)));
        }
        ArrayList<BoltPoint> branchPoints = new ArrayList<>();
        positions.stream().sorted().forEach(
            pos -> branchPoints.add(bolt.getBoltPoints().get(pos)));
        
        return branchPoints;
    }

    public void setBoltThickness(double strokeThickness) {
        boltStrokeWidth = strokeThickness;
        primaryBolt.setStrokeWidth(boltStrokeWidth);
    }
    public void setBranchThickness(double strokeThickness) {
        branchStrokeWidth = strokeThickness;
        branchList.forEach(bolt -> bolt.setStrokeWidth(branchStrokeWidth));
    }
    public void setEffect(Effect effect, Member member) {
        if(member == Member.PRIMARYBOLT) {
            primaryBolt.setEffect(effect);
        } else {
            branchList.forEach(branch -> branch.setEffect(effect));
        }
    }
            
    
//    public void setVisibleLength(int length) {
//        pointIndexProperty.set(length);
//    }
//    public int getVisibleLength() {
//        return pointIndexProperty.get();
//    }
    
//    public void animate(Duration animationDuration) {
//        
//        getPoints().clear();
//        pointIndexProperty.set(0);
//        Timeline timeline = new Timeline();
//        timeline.getKeyFrames().add(
//            new KeyFrame(milliseconds,
//                new KeyValue(pointIndexProperty, boltPoints.size(), Interpolator.EASE_OUT)
//            )
//        );
//        timeline.play();
//    }   
    
//    private void updateLine(int boltLength) {
//        //System.out.println("BoltLength: " + boltLength);
//        //Should we remove some points?
//        if(boltLength < (getPoints().size() / 2)) {
//            if(boltLength <= 0)
//                getPoints().clear();
//            else {
//                int start = 0;
//                int end = boltLength * 2;
//                getPoints().remove(start, end);
//            }
//        }
//        //should we add some points?
//        else if(boltLength > (getPoints().size() / 2)) {
//            Double [] points = new Double [boltLength * 2];
//            for(int i=0;i<boltLength;i++) {
//                points[i*2] = boltPoints.get(i).getX();
//                points[i*2+1] = boltPoints.get(i).getY();
//            }   
//            this.getPoints().setAll(points);
//        }
//    }
    
}