package net.gnehzr.cct.main;

import com.google.common.collect.ImmutableSet;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.XMLGuiMessages;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.misc.dynamicGUI.*;
import net.gnehzr.cct.statistics.SessionsList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * <p>
 * Created: 19.11.2014 1:15
 * <p>
 *
 * @author OneHalf
 */
class GuiParseSaxHandler extends DefaultHandler {

    private static final Logger LOG = LogManager.getLogger(GuiParseSaxHandler.class);
    public static final ImmutableSet<String> ELEMENTS_WITH_TEXT = ImmutableSet.of("label", "selectablelabel", "button", "checkbox", "menu", "menuitem", "checkboxmenuitem");

    private CALCubeTimerFrame calCubeTimerFrame;
    private int level = -2;
    private int componentID = -1;
    private List<String> strings;
    private List<JComponent> componentTree;
    private List<Boolean> needText;
    private List<String> elementNames;
    private JFrame frame;
    private final Configuration configuration;
    private final DynamicBorderSetter dynamicBorderSetter;
    private final XMLGuiMessages xmlGuiMessages;
    private final ActionMap actionMap;
    private final SessionsList sessionList;

    public GuiParseSaxHandler(CALCubeTimerFrame calCubeTimerFrame, JFrame frame, Configuration configuration,
                              DynamicBorderSetter dynamicBorderSetter, SessionsList sessionsList,
                              XMLGuiMessages xmlGuiMessages, ActionMap actionMap) {
        this.calCubeTimerFrame = calCubeTimerFrame;
        this.frame = frame;
        this.configuration = configuration;
        this.dynamicBorderSetter = dynamicBorderSetter;
        this.xmlGuiMessages = xmlGuiMessages;
        this.actionMap = actionMap;
        this.sessionList = sessionsList;

        componentTree = new ArrayList<>();
        strings = new ArrayList<>();
        needText = new ArrayList<>();
        elementNames = new ArrayList<>();

        calCubeTimerFrame.tabbedPanes.clear();
        calCubeTimerFrame.splitPanes.clear();

        calCubeTimerFrame.dynamicStringComponents.destroy();
    }

    @Override
    public void startElement(String namespaceURI, String lName, String qName, Attributes attrs) throws SAXException {
        componentID++;
        level++;
        String elementName = qName.toLowerCase();

        if (level == -1) {
            checkRootLevelTag(elementName);
            return;
        }

        if (level == 0) {
            checkFirstLevelTag(elementName);
        }

        //must deal with level < 0 before adding anything
        elementNames.add(elementName);
        needText.add(ELEMENTS_WITH_TEXT.contains(elementName));
        strings.add("");

        String temp;
        JComponent component;

        try {
            component = parseComponent(attrs, elementName);
        } catch (Exception e) {
            throw new SAXException(e);
        }

        componentTree.add(component);

        if (level == 0) {
            if (elementName.equals("panel")) {
                frame.setContentPane(componentTree.get(level));
            } else if (elementName.equals("menubar")) {
                frame.setJMenuBar((JMenuBar) componentTree.get(level));
            }
        } else if (component != null) {
            temp = null;
            for (int i = level - 1; i >= 0; i--) {
                JComponent c = componentTree.get(i);
                if (c != null) {
                    if (temp == null) {
                        if (c instanceof JScrollPane) {
                            ((JScrollPane) c).setViewportView(component);
                        } else if (c instanceof JSplitPane) {
                            JSplitPane jsp = (JSplitPane) c;
                            if (jsp.getLeftComponent() == null) {
                                ((JSplitPane) c).setLeftComponent(component);
                            } else {
                                ((JSplitPane) c).setRightComponent(component);
                            }
                        } else if (c instanceof JTabbedPane) {
                            ((JTabbedPane) c).addTab(component.getName(), component);
                        } else
                            c.add(component);
                    } else {
                        String loc = null;
                        switch (temp) {
                            case "center":
                                loc = BorderLayout.CENTER;
                                break;
                            case "east":
                                loc = BorderLayout.EAST;
                                break;
                            case "west":
                                loc = BorderLayout.WEST;
                                break;
                            case "south":
                                loc = BorderLayout.SOUTH;
                                break;
                            case "north":
                                loc = BorderLayout.NORTH;
                                break;
                            case "page_start":
                                loc = BorderLayout.PAGE_START;
                                break;
                            case "page_end":
                                loc = BorderLayout.PAGE_END;
                                break;
                            case "line_start":
                                loc = BorderLayout.LINE_START;
                                break;
                            case "line_end":
                                loc = BorderLayout.LINE_END;
                                break;
                        }
                        c.add(component, loc);
                    }
                    break;
                } else temp = elementNames.get(i);
            }
        }

        if (component instanceof DynamicStringSettable)
            calCubeTimerFrame.dynamicStringComponents.registerDynamicComponent((DynamicStringSettable) component);
    }

    private JComponent parseComponent(Attributes attrs, String elementName) throws SAXException {
        JComponent component;
        String temp;
        switch (elementName) {
            case "label":
                component = new DynamicLabel();
                break;
            case "selectablelabel":
                component = new DynamicSelectableLabel();
                if ((temp = attrs.getValue("editable")) != null) {
                    ((DynamicSelectableLabel) component).setEditable(Boolean.parseBoolean(temp));
                }
                break;
            case "button":
                component = new DynamicButton();
                component.setFocusable(configuration.getBoolean(VariableKey.FOCUSABLE_BUTTONS));
                break;
            case "checkbox":
                component = new DynamicCheckBox();
                component.setFocusable(configuration.getBoolean(VariableKey.FOCUSABLE_BUTTONS));
                break;
            case "panel":
                component = parsePanel(attrs);
                break;
            case "component":
                if (attrs == null || (temp = attrs.getValue("type")) == null) {
                    throw new SAXException("parse error in component");
                }
                component = calCubeTimerFrame.persistentComponents.getComponent(temp);
                if (component == null) {
                    throw new SAXException("could not find component: " + temp.toLowerCase());
                }
                break;
            case "center":
            case "east":
            case "west":
            case "south":
            case "north":
            case "page_start":
            case "page_end":
            case "line_start":
            case "line_end":
                component = null;
                break;
            case "menubar":
                component = parseMenuBar();
                break;
            case "menu":
                JMenu menu = new DynamicMenu();
                if ((temp = attrs.getValue("mnemonic")) != null) {
                    menu.setMnemonic(temp.charAt(0));
                }
                component = menu;
                break;
            case "menuitem":
                component = new DynamicMenuItem(configuration);
                break;
            case "checkboxmenuitem":
                component = new DynamicCheckBoxMenuItem(configuration);
                break;
            case "separator":
                component = new JSeparator();
                break;
            case "scrollpane":
                component = parseScrollPane();
                break;
            case "tabbedpane":
                component = new DynamicTabbedPane(configuration, xmlGuiMessages, sessionList);
                component.setName(Integer.toString(componentID));
                calCubeTimerFrame.tabbedPanes.add((JTabbedPane) component);
                break;
            case "splitpane":
                component = new JSplitPane(parseSplitOrientation(attrs.getValue("split")), true, null, null);
                component.setName(Integer.toString(componentID));
                calCubeTimerFrame.splitPanes.add((JSplitPane) component);
                break;
            case "glue":
                component = parseGlue(attrs);
                break;
            default:
                throw new SAXException("invalid tag " + elementName);
        }

        if (component instanceof AbstractButton) {
            if (attrs != null) {
                if ((temp = attrs.getValue("action")) != null) {
                    AbstractAction a = actionMap.getAction(temp);
                    if (a != null) ((AbstractButton) component).setAction(a);
                    else throw new SAXException("parse error in action: " + temp.toLowerCase());
                }
            }
        }

        if (component != null && attrs != null) {
            try {
                if ((temp = attrs.getValue("alignmentX")) != null) {
                    component.setAlignmentX(Float.parseFloat(temp));
                }
                if ((temp = attrs.getValue("alignmentY")) != null) {
                    component.setAlignmentY(Float.parseFloat(temp));
                }
                if ((temp = attrs.getValue("border")) != null) {
                    component.setBorder(dynamicBorderSetter.getBorder(temp));
                }
                if ((temp = attrs.getValue("minimumsize")) != null) {
                    String[] dims = temp.split("x");
                    component.setMinimumSize(new Dimension(Integer.parseInt(dims[0]), Integer.parseInt(dims[1])));
                }
                if ((temp = attrs.getValue("preferredsize")) != null) {
                    String[] dims = temp.split("x");
                    component.setPreferredSize(new Dimension(Integer.parseInt(dims[0]), Integer.parseInt(dims[1])));
                }
                if ((temp = attrs.getValue("maximumsize")) != null) {
                    String[] dims = temp.split("x");
                    component.setMaximumSize(new Dimension(Integer.parseInt(dims[0]), Integer.parseInt(dims[1])));
                }
                if (component instanceof JScrollPane) {
                    JScrollPane scroller = (JScrollPane) component;
                    if ((temp = attrs.getValue("verticalpolicy")) != null) {
                        int policy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
                        if (temp.equalsIgnoreCase("never"))
                            policy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;
                        else if (temp.equalsIgnoreCase("always"))
                            policy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;
                        scroller.setVerticalScrollBarPolicy(policy);
                    }
                    if ((temp = attrs.getValue("horizontalpolicy")) != null) {
                        int policy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
                        if (temp.equalsIgnoreCase("never"))
                            policy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
                        else if (temp.equalsIgnoreCase("always"))
                            policy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
                        scroller.setHorizontalScrollBarPolicy(policy);
                    }
                } else if (component instanceof JSplitPane) {
                    JSplitPane jsp = (JSplitPane) component;
                    if ((temp = attrs.getValue("drawcontinuous")) != null) {
                        jsp.setContinuousLayout(Boolean.parseBoolean(temp));
                    }
                    if ((temp = attrs.getValue("resizeweight")) != null) {
                        double resizeWeight;
                        try {
                            resizeWeight = Double.parseDouble(temp);
                        } catch (Exception e) {
                            resizeWeight = .5;
                        }
                        jsp.setResizeWeight(resizeWeight);
                    }
                    if ((temp = attrs.getValue("split")) != null) {
                        jsp.setOrientation(parseSplitOrientation(temp));
                    }
                }
                if ((temp = attrs.getValue("opaque")) != null)
                    component.setOpaque(Boolean.parseBoolean(temp));
                if ((temp = attrs.getValue("background")) != null)
                    component.setBackground(Utils.stringToColor(temp, false));
                if ((temp = attrs.getValue("foreground")) != null)
                    component.setForeground(Utils.stringToColor(temp, false));
                if ((temp = attrs.getValue("orientation")) != null) {
                    if (component instanceof JSeparator) {
                        if (temp.equalsIgnoreCase("horizontal")) {
                            ((JSeparator) component).setOrientation(SwingConstants.HORIZONTAL);
                        }
                        else if (temp.equalsIgnoreCase("vertical")) {
                            ((JSeparator) component).setOrientation(SwingConstants.VERTICAL);
                        }
                    } else if (component instanceof JSplitPane) {
                        JSplitPane jsp = (JSplitPane) component;
                        jsp.setOrientation(parseSplitOrientation(temp));
                    }
                }
                if ((temp = attrs.getValue("nominsize")) != null) {
                    component.putClientProperty(SubstanceLookAndFeel.BUTTON_NO_MIN_SIZE_PROPERTY, Boolean.parseBoolean(temp));
                }
                if ((temp = attrs.getValue("name")) != null) {
                    component.setName(temp);
                }
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }
        return component;
    }

    private int parseSplitOrientation(String temp) {
        return temp.equals("horizontal") ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT;
    }

    @NotNull
    private JComponent parseGlue(Attributes attrs) throws SAXException {
        Component glue = parseGlueOrientation(attrs.getValue("orientation"));
        JComponent component = new JPanel();
        component.add(glue);
        return component;
    }

    @NotNull
    private Component parseGlueOrientation(String orientation) throws SAXException {
        if (orientation == null) {
            return Box.createGlue();
        }

        switch (orientation) {
            case "horizontal" :
                return Box.createHorizontalGlue();
            case "vertical" :
                return Box.createVerticalGlue();
            default:
                throw new SAXException("parse error in orientation");
        }
    }

    @NotNull
    private JComponent parseScrollPane() {
        return new JScrollPane() {
            {
                setBorder(null);
            }

            @Override
            public void updateUI() {
                Border t = getBorder();
                super.updateUI();
                setBorder(t);
            }

            @Override
            public Dimension getPreferredSize() {
                Insets i = this.getInsets();
                Dimension d = getViewport().getView().getPreferredSize();
                return new Dimension(d.width + i.left + i.right, d.height + i.top + i.bottom);
            }

            @Override
            public Dimension getMinimumSize() {
                //this is to allow "nice" gui resizing
                return new Dimension(0, 0);
            }
        };
    }

    @NotNull
    private JComponent parseMenuBar() {
        JComponent component;
        component = new JMenuBar() {
            @Override
            public Component add(Component comp) {
                //if components in the menubar resist resizing,
                //it prevents the whole gui from resizing
                //the minimum height is 10 because buttons were
                //acting weird if it was smaller
                comp.setMinimumSize(new Dimension(1, 10));
                return super.add(comp);
            }
        };
        return component;
    }

    private JComponent parsePanel(Attributes attrs) throws SAXException {
        JComponent component;
        String temp;
        component = new JPanel() {
            //we're overwriting this to allow "nice" resizing of the gui with the jsplitpane
            @Override
            public Dimension getMinimumSize() {
                Object o = this.getClientProperty(SubstanceLookAndFeel.BUTTON_NO_MIN_SIZE_PROPERTY);
                if (o != null && !((Boolean) o))
                    return super.getMinimumSize();

                return new Dimension(0, 0);
            }
        };
        int hgap = 0;
        int vgap = 0;
        int align = FlowLayout.CENTER;
        int rows = 0;
        int cols = 0;
        int orientation = BoxLayout.Y_AXIS;

        LayoutManager layout;
        if (attrs == null) layout = new FlowLayout();
        else {
            try {
                if ((temp = attrs.getValue("hgap")) != null) hgap = Integer.parseInt(temp);
                if ((temp = attrs.getValue("vgap")) != null) vgap = Integer.parseInt(temp);
                if ((temp = attrs.getValue("rows")) != null) rows = Integer.parseInt(temp);
                if ((temp = attrs.getValue("cols")) != null) cols = Integer.parseInt(temp);
            } catch (Exception e) {
                throw new SAXException("integer parse error", e);
            }

            if ((temp = attrs.getValue("align")) != null) {
                if (temp.equalsIgnoreCase("left")) align = FlowLayout.LEFT;
                else if (temp.equalsIgnoreCase("right")) align = FlowLayout.RIGHT;
                else if (temp.equalsIgnoreCase("center")) align = FlowLayout.CENTER;
                else if (temp.equalsIgnoreCase("leading")) align = FlowLayout.LEADING;
                else if (temp.equalsIgnoreCase("trailing")) align = FlowLayout.TRAILING;
                else throw new SAXException("parse error in align");
            }

            if ((temp = attrs.getValue("orientation")) != null) {
                if (temp.equalsIgnoreCase("horizontal")) orientation = BoxLayout.X_AXIS;
                else if (temp.equalsIgnoreCase("vertical")) orientation = BoxLayout.Y_AXIS;
                else if (temp.equalsIgnoreCase("page")) orientation = BoxLayout.PAGE_AXIS;
                else if (temp.equalsIgnoreCase("line")) orientation = BoxLayout.LINE_AXIS;
                else throw new SAXException("parse error in orientation");
            }

            if ((temp = attrs.getValue("layout")) != null) {
                if (temp.equalsIgnoreCase("border")) layout = new BorderLayout(hgap, vgap);
                else if (temp.equalsIgnoreCase("box")) layout = new BoxLayout(component, orientation);
                else if (temp.equalsIgnoreCase("grid")) layout = new GridLayout(rows, cols, hgap, vgap);
                else if (temp.equalsIgnoreCase("flow")) layout = new FlowLayout(align, hgap, vgap);
                else throw new SAXException("parse error in layout");
            } else
                layout = new FlowLayout(align, hgap, vgap);
        }

        component.setLayout(layout);
        return component;
    }

    private void checkFirstLevelTag(String elementName) throws SAXException {
        if (!(elementName.equals("menubar") || elementName.equals("panel"))) {
            throw new SAXException("parse error: level 1 must be menubar or panel");
        }
    }

    private void checkRootLevelTag(String elementName) throws SAXException {
        if (!elementName.equals("gui")) {
            throw new SAXException("parse error: invalid root tag");
        }
    }

    @Override
    public void endElement(String namespaceURI, String sName, String qName) throws SAXException {
        if (level >= 0) {
            if (needText.get(level) && strings.get(level).length() > 0) {
                if (componentTree.get(level) instanceof DynamicStringSettable)
                    ((DynamicStringSettable) componentTree.get(level)).setDynamicString(new DynamicString(strings.get(level), xmlGuiMessages, configuration));
            }
            if (componentTree.get(level) instanceof JTabbedPane) {
                JTabbedPane temp = (JTabbedPane) componentTree.get(level);
                Integer t = configuration.getInt(VariableKey.JCOMPONENT_VALUE(temp.getName(), true, configuration.getXMLGUILayout()));
                if (t != null) {
                    temp.setSelectedIndex(t);
                }
            }
            componentTree.remove(level);
            elementNames.remove(level);
            strings.remove(level);
            needText.remove(level);
        }
        level--;
    }

    @Override
    public void characters(char buf[], int offset, int len) throws SAXException {
        if (level >= 0 && needText.get(level)) {
            String s = new String(buf, offset, len);
            if (!s.trim().isEmpty()) strings.set(level, strings.get(level) + s);
        }
    }

    @Override
    public void error(SAXParseException e) throws SAXParseException {
        warning(e); //I figure that anyone messing around with xml guis will have to be proficient enough to handle the command line
    }

    @Override
    public void warning(SAXParseException e) throws SAXParseException {
        LOG.error(e.getSystemId() + ":" + e.getLineNumber() + ": warning: " + e.getMessage());
    }
}