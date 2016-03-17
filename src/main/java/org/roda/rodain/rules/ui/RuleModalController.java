package org.roda.rodain.rules.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import org.roda.rodain.core.RodaIn;
import org.roda.rodain.rules.MetadataTypes;
import org.roda.rodain.rules.Rule;
import org.roda.rodain.rules.RuleTypes;
import org.roda.rodain.rules.VisitorStack;
import org.roda.rodain.rules.sip.SipPreview;
import org.roda.rodain.rules.sip.SipPreviewCreator;
import org.roda.rodain.schema.ui.SchemaNode;
import org.roda.rodain.source.ui.items.SourceTreeItem;
import org.roda.rodain.utils.TreeVisitor;
import org.roda.rodain.utils.WalkFileTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 19-10-2015.
 */
public class RuleModalController {
  private static final Logger log = LoggerFactory.getLogger(RuleModalController.class.getName());
  private static RuleModalStage stage;
  private static RuleModalPane pane;
  private static Set<SourceTreeItem> sourceSet;
  private static SchemaNode schema;

  private static VisitorStack visitors = new VisitorStack();

  private RuleModalController() {

  }

  /**
   * Creates the scene to show the modal window with the options to create a new
   * Rule.
   *
   * @param primStage
   *          The main stage of the application.
   * @param source
   *          The set of items chosen by the user to create the new association.
   * @param schemaNode
   *          The destination of the SIPs that will be created.
   */
  public static void newAssociation(final Stage primStage, Set<SourceTreeItem> source, SchemaNode schemaNode) {
    if (stage == null)
      stage = new RuleModalStage(primStage);
    stage.setWidth(800);
    stage.setHeight(580);

    LoadingPane loadingPane = new LoadingPane(schemaNode);
    stage.setRoot(loadingPane);

    sourceSet = source;
    schema = schemaNode;

    Task<Void> task = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        pane = new RuleModalPane(primStage, sourceSet, schema);
        return null;
      }
    };

    task.setOnSucceeded(event -> Platform.runLater(() -> stage.setRoot(pane)));

    new Thread(task).start();
  }

  /**
   * Confirms the creation of a new Rule.
   * <p/>
   * <p>
   * The method starts by getting the association and metadata types chosen by
   * the user. Then creates a new Rule using that information and, if it
   * applies, the options from each chosen type (file names, maximum folder
   * depth, etc.).
   * </p>
   * <p>
   * The next step is to create the tree visitor to walk the files tree and add
   * it to the queue of visitors. Finally, notifies the other components of the
   * interface that a new association has been created and they need to adapt to
   * it.
   * </p>
   */
  public static void confirm() {
    try {
      RuleTypes assocType = pane.getAssociationType();
      MetadataTypes metaType = pane.getMetadataType();
      Path metadataPath = null;
      String templateType = null;
      String templateVersion = null;
      switch (metaType) {
        case DIFF_DIRECTORY:
          metadataPath = pane.getDiffDir();
          break;
        case SINGLE_FILE:
          metadataPath = pane.getFromFile();
          break;
        case SAME_DIRECTORY:
          templateType = pane.getSameFolderPattern();
          break;
        case TEMPLATE:
          String template = pane.getTemplate();
          String[] splitted = template.split("!###!");
          templateType = splitted[0];
          templateVersion = splitted[1];
          break;
        default:
          break;
      }

      Rule rule = new Rule(sourceSet, assocType, metadataPath, templateType, metaType, templateVersion);
      rule.addObserver(schema);

      TreeVisitor visitor = rule.apply();

      // create set with the selected paths
      Set<String> sourcePaths = new HashSet<>();
      for (SourceTreeItem sti : sourceSet) {
        sourcePaths.add(sti.getPath());
        rule.addObserver(sti);
      }

      WalkFileTree fileWalker = visitors.add(sourcePaths, visitor);

      schema.addRule(rule);

      Platform.runLater(() -> {
        RuleModalProcessing processing = new RuleModalProcessing((SipPreviewCreator) visitor, visitor, visitors,
          fileWalker);
        stage.setRoot(processing);
        stage.setHeight(180);
        stage.setWidth(400);
        stage.centerOnScreen();
        RodaIn.getSchemePane().showTree();
      });
    } catch (Exception e) {
      log.error("Exception in confirm rule", e);
    }
  }

  /**
   * Creates a new RuleModalRemoving pane and sets the stage's root scene as
   * that pane.
   *
   * @param r
   *          The rule to be removed
   * @see RuleModalRemoving
   */
  public static void removeRule(Rule r) {
    RuleModalRemoving removing;
    if (stage.isShowing() && stage.getScene().getRoot() instanceof RuleModalRemoving) {
      removing = (RuleModalRemoving) stage.getScene().getRoot();
      r.addObserver(removing);
    } else {
      removing = new RuleModalRemoving();
      r.addObserver(removing);
      stage.setRoot(removing);
      stage.setHeight(120);
      stage.setWidth(400);
      stage.centerOnScreen();
    }
    removing.addRule(r);
  }

  /**
   * Creates a new RuleModalRemoving pane and sets the stage's root scene as
   * that pane.
   *
   * @param sip
   *          The SIP to be removed
   * @see RuleModalRemoving
   */
  public static void removeSipPreview(SipPreview sip) {
    RuleModalRemoving removing;
    if (stage.isShowing() && stage.getScene().getRoot() instanceof RuleModalRemoving) {
      removing = (RuleModalRemoving) stage.getScene().getRoot();
      sip.addObserver(removing);
    } else {
      removing = new RuleModalRemoving();
      sip.addObserver(removing);
      stage.setRoot(removing);
      stage.setHeight(120);
      stage.setWidth(400);
      stage.centerOnScreen();
    }
    removing.addSIP(sip);
  }

  /**
   * Closes the stage of the modal window.
   */
  public static void cancel() {
    Platform.runLater(() -> stage.close());
  }
}
