package lit.litfx.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.Node;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.effect.SepiaTone;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import lit.litfx.core.components.AnimatedEffect;
import lit.litfx.core.components.Arc;
import lit.litfx.core.components.BoltDynamics;
import lit.litfx.core.components.ChainLightning;
import lit.litfx.core.components.NodeLink;

/**
 *
 * @author Birdasaur
 * Manages Lit components and tracks any associated Nodes
 */
public class LitView extends Region {
    /**
     * Provides lookup mechanism to find any Node that is currently 
     * tracked in the system.
     */    
    private HashMap<NodeLink, AnimatedEffect> nodeLinkToEffectMap = new HashMap<>();
    private Region litRegion;
    public SimpleDoubleProperty boltAnimationDuration = new SimpleDoubleProperty(100);
    public SimpleLongProperty transitionDelay = new SimpleLongProperty(100);
    public SimpleLongProperty animationSleepDelay = new SimpleLongProperty(10);
            
    public LitView(Region parentToOverlay) {
        litRegion = parentToOverlay;
        prefWidthProperty().bind(litRegion.widthProperty());
        prefHeightProperty().bind(litRegion.heightProperty());
        setMouseTransparent(true);
    }
    
    public ChainLightning chainNodes(ArrayList<Node> nodes, 
            BoltDynamics boltDynamics, BoltDynamics loopDynamics) {
        return chainNodes(nodes, boltDynamics, loopDynamics, false);
    }
    public ChainLightning chainNodes(ArrayList<Node> nodes,
        BoltDynamics boltDynamics, BoltDynamics loopDynamics, boolean animate) {
        //@TODO SMP Maybe we should recognize the NodeLink here from first to last
        //rather than first to second?
        NodeLink nodeLink = new NodeLink(nodes.get(0), nodes.get(1));
        if(nodeLinkToEffectMap.containsKey(nodeLink)) {
            nodeLinkToEffectMap.get(nodeLink).stop();
            getChildren().remove(nodeLinkToEffectMap.get(nodeLink));
        }

        ChainLightning cl = new ChainLightning(nodes, boltDynamics, loopDynamics);
        cl.setEffect(getBoltEffects());
        cl.setStroke(Color.ALICEBLUE);
        cl.setOpacity(0.75);
        cl.setStrokeWidth(4.0); 
        nodeLinkToEffectMap.put(nodeLink, cl);
        //If we intend to animate first set all the sub bolts to be not visible
        if(animate)
            cl.setVisibleLength(0);
        getChildren().add(cl);
        //if animating, initiate the animation task
        if(animate)
            cl.animate(Duration.millis(boltAnimationDuration.get()), 
                transitionDelay.get(), animationSleepDelay.get());
        return cl;
    }
    public Arc arcNodes(Node node1, Node node2, BoltDynamics dynamics) {
        NodeLink nodeLink = new NodeLink(node1, node2);
        if(nodeLinkToEffectMap.containsKey(nodeLink)) {
            getChildren().remove(nodeLinkToEffectMap.get(nodeLink));
        }
        Arc arc = new Arc(node1, node2, dynamics); 
        arc.setEffect(getBoltEffects());
        arc.setStroke(Color.ALICEBLUE);
        arc.setOpacity(0.75);
        arc.setStrokeWidth(4.0);        
        arc.setVisibleLength(0);
        getChildren().add(arc);
        arc.animate(Duration.millis(boltAnimationDuration.get()));
        nodeLinkToEffectMap.put(nodeLink, arc);
        return arc;
    }
    
    private Effect getBoltEffects() {
        SepiaTone st = new SepiaTone(0.25);
        Bloom bloom = new Bloom(0.25);
        bloom.setInput(st);
        Glow glow = new Glow(0.75);
        glow.setInput(bloom);
        DropShadow shadow = new DropShadow(BlurType.GAUSSIAN, Color.CORNSILK, 10, 0.5, 0, 0);
        shadow.setInput(glow);
        shadow.setRadius(60.0);
        return shadow;
    }    
    
    /**
     * Provides lookup mechanism to find any edge that is currently 
     * managed in the view.
     * @param node1 the Node to Node connection that has an AnimatedEdge 
     * @param node2 the second node in this connection
     * @return The AnimatedEffect. If no NodeLink exists that matches then returns null.
     */
    public AnimatedEffect lookupByNode(Node node1, Node node2) {
        return nodeLinkToEffectMap.get(new NodeLink(node1, node2));
    }    

    /**
     * Remove a Node to Node AnimatedEdge from the view. 
     * 
     * @param node1 The first Node in the NodeLink
     * @param node2 The second Node in the NodeLink
     */     
    public void removeArc(Node node1, Node node2) {
        NodeLink nodeLink = new NodeLink(node1, node2);
        if(nodeLinkToEffectMap.containsKey(nodeLink)) {
            getChildren().remove(nodeLinkToEffectMap.get(nodeLink));
            nodeLinkToEffectMap.remove(nodeLink);
        }
    }     
    /**
     * Removes all model objects currently in the mapping. 
     * 
     */     
    public void clearAll() {
        nodeLinkToEffectMap.clear();
        getChildren().clear();
    }
    /**
     * Get a list of all the AnimatedEdge objects in the mapping
     * @return Collection of AnimatedEffect objects in the mapping
     */ 
    public Collection<AnimatedEffect> getEffects() {
        return nodeLinkToEffectMap.values();
    }    
}