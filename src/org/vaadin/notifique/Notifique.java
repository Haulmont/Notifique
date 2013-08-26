package org.vaadin.notifique;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.Reindeer;
import org.vaadin.jouni.animator.AnimatorProxy;
import org.vaadin.jouni.animator.shared.AnimType;

public class Notifique extends CustomComponent {

    /**
     * Built-in message styles.
     *
     */
    public interface Styles extends Serializable{
        static String INFO = "info";
        static String SUCCESS = "success";
        static String WARNING = "warning";
        static String ERROR = "error";
        static String MESSAGE = "message";
        static String MAGIC_BLACK = "magic-black";
        static String MAGIC_GRAY = "magic-gray";
        static String MAGIC_WHITE = "magic-white";
        static String BROWSER_FF = "ff";
        static String BROWSER_FF3 = "ff3";
        static String BROWSER_IE = "ie";
        static String BROWSER_CHROME = "chrome";
        static String VAADIN_BLACK = "vaadin-black";
        static String VAADIN_BEIGE = "vaadin-beige";
        static String VAADIN_RED = "vaadin-red";
        static String VAADIN_GREEN = "vaadin-green";
        static String VAADIN_BLUE = "vaadin-blue";
        static String VAADIN_ORANGE = "vaadin-orange";
    };

    /**
     * Listener interface for message clicks.
     *
     */
    public interface ClickListener extends Serializable {

        public void messageClicked(Message message);

    }

    /**
     * Listener interface for message close events.
     *
     */
    public interface HideListener extends Serializable {

        public void messageHide(Message message);

    }

    private static final String STYLE_QUEUE = "queue";
    private static final String STYLE_ITEM = "item";
    private static final String STYLE_CLOSE = "close";

    private static final long serialVersionUID = -4838466600618368413L;

    private Panel root;
    private CssLayout css;
    private List<Message> items = new LinkedList();
    private boolean autoScroll;
    private int visibleCount = 5;
    private boolean fillFromTop = false;
    private HideListener hideListener;
    private ClickListener clickListener;
    private AnimatorProxy ap;

    public class Message implements Serializable {
        private static final long serialVersionUID = 5892777954593320723L;
        private Component component;
        private CssLayout animatedContent;
        private boolean visible;
        private Object data;

        private void show() {
            ap.animate(animatedContent, AnimType.ROLL_DOWN_OPEN).setDuration(200)
                    .setDelay(0);

            visible = true;
        }

        public void hide() {
            if (!isVisible()) {
                return;
            }
            ap.animate(animatedContent, AnimType.ROLL_UP_CLOSE_REMOVE).setDuration(200)
                    .setDelay(0);
            visible = false;
            if (getHideListener() != null) {
                getHideListener().messageHide(this);
            }
        }

        public Component getComponent() {
            return component;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public Object getData() {
            return data;
        }

        public Notifique getNotifique() {
            return Notifique.this;
        }

    }

    /**
//     * Custom animator extension allowing us to react on hide events coming from
//     * client-side.
//     *
//     * We use this to actually remove the components from layout after they have
//     * been animated away.
//     *
//     * @author Sami Ekblad
//     *
//     */
//    public class MessageAnimator extends Animator {
//        private static final long serialVersionUID = 1L;
//
//        @Override
//        public void changeVariables(Object source, Map<String, Object> variables) {
//            boolean wasHidden = isRolledUp();
//            super.changeVariables(source, variables);
//            if (!wasHidden && isRolledUp()) {
//                removeAfterHide(this);
//            } else if (!isRolledUp() && !fillFromTop) {
//                root.setScrollTop(10000);
//                root.requestRepaint(); // TODO
//            }
//        }
//
//        public MessageAnimator(Component toAnimate) {
//            super(toAnimate);
//            setWidth("100%");
//        }
//    }

    public Notifique(boolean autoScroll) {
        ap = new AnimatorProxy();
        css = new CssLayout();
        root = new Panel(css);
        root.setStyleName(Reindeer.PANEL_LIGHT);
        css.setWidth("100%");
        css.addComponent(ap);
        ap.addListener(new AnimatorProxy.AnimationListener() {
            @Override
            public void onAnimation(AnimatorProxy.AnimationEvent animationEvent) {
                if (animationEvent.getAnimation().getType().equals(AnimType.ROLL_LEFT_CLOSE_REMOVE)) {
                    items.remove(animationEvent.getComponent());
                }
            }
        });
        root.addStyleName(STYLE_QUEUE);
        root.getContent().setStyleName(STYLE_QUEUE);
        setCompositionRoot(root);
        this.autoScroll = autoScroll;
    }

    /**
     * Publish a message.
     *
     * @param m
     */
    protected void publish(Message m) {
        synchronized (items) {
            if (fillFromTop) {
                items.add(0, m);
                ((CssLayout) root.getContent())
                        .addComponentAsFirst(m.animatedContent);
            } else {
                items.add(m);
                css.addComponent(m.animatedContent);
            }
            m.show();
            if (autoScroll && items.size() > getVisibleCount()) {
                Message hideThis = fillFromTop ? items.get(items.size() - 1)
                        : items.get(0);
                items.remove(hideThis);
                hideThis.hide();
            }
        }
    }

//    /**
//     * Remove the item after it has been hidden.
//     *
//     * @param toBeRemoved
//     */
//    private void removeAfterHide(MessageAnimator toBeRemoved) {
//        synchronized (items) {
//            Message removed = null;
//            for (Message m : items) {
//                if (m.animatedContent.equals(toBeRemoved)) {
//                    removed = m;
//                    break;
//                }
//            }
//
//            if (removed != null) {
//                css.removeComponent(removed.animatedContent);
//                items.remove(removed);
//            }
//        }
//    }

    /**
     * Create a new item into the queue. It is not visible until added with
     *
     * @param style
     *
     * @param animation
     * @param string
     * @return
     */
    protected Message createMessage(final Component component, String style) {

        final Message m = new Message();

        // Wrap to a CssLayout (this is needed for styling and sizing correctly)
        CssLayout css = new CssLayout();
        css.setWidth("100%");
        css.setStyleName(Notifique.STYLE_ITEM);
        css.addStyleName(style != null? style: Styles.MESSAGE);
        css.addComponent(component);

        // Wrap component into an animator
        m.component = component;
        m.animatedContent = css;

        return m;
    }

    public Message add(Resource icon, String htmlMessage, String style) {
        return add(icon, htmlMessage, true, style, true);
    }

    public Message add(Resource icon, String message, boolean allowHTML,
            String style, boolean showCloseButton) {

        // A Label for the message
        Component l = createContentFor(message, allowHTML);
        return add(icon, l, style, showCloseButton);
    }

    public Message add(Resource icon, Component component, String style,
            boolean showCloseButton) {

        HorizontalLayout lo = new HorizontalLayout();
        lo.setSpacing(true);
        lo.setWidth("100%");

        final Message i = createMessage(lo, style);
        publish(i);

        // Add icon if given
        if (icon != null) {
            lo.addComponent(new Embedded(null, icon));
        }

        lo.addComponent(component);
        lo.setExpandRatio(component, 1f);
        lo.setComponentAlignment(component, Alignment.MIDDLE_LEFT);

        // Close button if requested
        if (showCloseButton) {
            Button close = createCloseButtonFor(i);
            if (close != null) {
                lo.addComponent(close);
            }
        }

        // Listen for clicks
        lo.addListener(new LayoutClickListener() {
            private static final long serialVersionUID = 7524442205441374595L;

            public void layoutClick(LayoutClickEvent event) {
                if (getClickListener() != null) {
                    getClickListener().messageClicked(i);
                }
            }
        });

        return i;
    }

    /**
     * Remove all messages.
     *
     */
    public void clear() {
        synchronized (items) {
            final LinkedList<Component> l = new LinkedList<Component>();

            for (final Iterator<Component> i = css.getComponentIterator(); i.hasNext();) {
                l.add(i.next());
            }

            for (final Iterator<Component> i = l.iterator(); i.hasNext();) {
                Component component = i.next();
                if (component instanceof CssLayout) {
                    css.removeComponent(component);
                }
            }
            items.clear();
        }
    }

    /**
     * Get current size of the queue.
     *
     * @return
     */
    public int size() {
        synchronized (items) {
            return items.size();
        }
    }

    /**
     * Set number of messages visible at once.
     *
     * This is only meaningful if autoScroll is true.
     *
     * @param visibleItems
     */
    public void setVisibleCount(int visibleItems) {
        visibleCount = visibleItems;
    }

    /**
     * Add new items to the top rather than the end of the list.
     *
     * @param fillFromTop
     */
    public void setFillFromTop(boolean fillFromTop) {
        this.fillFromTop = fillFromTop;
    }

    /**
     * Add new items to the top rather than the end of the list.
     *
     * @return
     */
    public boolean isFillFromTop() {
        return fillFromTop;
    }

    /**
     * Get number of messages visible at once.
     *
     * This is only meaningful if autoScroll is true.
     */
    public int getVisibleCount() {
        return visibleCount;
    }

    @Override
    public void setHeight(String height) {
        super.setHeight(height);
        root.setHeight(height);
    }

    @Override
    public void setWidth(String width) {
        super.setWidth(width);
        root.setWidth(width);
    }

    /**
     * Create a close button for a message.
     *
     * @param i
     * @return
     */
    protected Button createCloseButtonFor(final Message i) {
        Button b = new Button();
        b.addListener(new Button.ClickListener() {
            private static final long serialVersionUID = -1932127150282887613L;

            public void buttonClick(ClickEvent event) {
                i.hide();

            }
        });
        b.setStyleName(Reindeer.BUTTON_LINK);
        b.addStyleName(STYLE_CLOSE);
        return b;
    }

    protected Component createContentFor(String string, boolean allowHTML) {
        Label l = new Label(string, allowHTML ? Label.CONTENT_RAW
                : Label.CONTENT_TEXT);
        return l;
    }

    /**
     * Set the click listener to receive item (message) hide events.
     *
     * @param hideListener
     */

    public void setHideListener(HideListener hideListener) {
        this.hideListener = hideListener;
    }

    public HideListener getHideListener() {
        return hideListener;
    }

    /**
     * Set the click listener to receive item (message) clikcs.
     *
     * @param clickListener
     */
    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public ClickListener getClickListener() {
        return clickListener;
    }

}
