package org.ebookdroid.ui.viewer.adapters;

import com.greenlemonmobile.app.ebook.R;
import org.ebookdroid.core.codec.OutlineLink;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class OutlineAdapter extends BaseAdapter {

    private int spaceWidth;

    //private final Drawable background;
    private final Drawable selected;

    private final VoidListener voidListener = new VoidListener();
    private final ItemListener itemListener = new ItemListener();
    private final CollapseListener collapseListener = new CollapseListener();

    private final Context context;
    private final OutlineLink[] objects;
    private final OutlineItemState[] states;
    private final SparseIntArray mapping = new SparseIntArray();
    private final int currentId;

    public OutlineAdapter(final Context context, final List<OutlineLink> objects, final OutlineLink current) {
        this.context = context;
        final Resources resources = context.getResources();
        //background = resources.getDrawable(R.drawable.pressed_list_item_background);
        selected = resources.getDrawable(R.drawable.pressed_list_item_background);

        this.objects = objects.toArray(new OutlineLink[objects.size()]);
        this.states = new OutlineItemState[this.objects.length];

        boolean treeFound = false;
        for (int i = 0; i < this.objects.length; i++) {
            mapping.put(i, i);
            final int next = i + 1;
            if (next < this.objects.length && this.objects[i].level < this.objects[next].level) {
                states[i] = OutlineItemState.COLLAPSED;
                treeFound = true;
            } else {
                states[i] = OutlineItemState.LEAF;
            }
        }

        currentId = current != null ? objects.indexOf(current) : -1;

        if (treeFound) {
            for (int parent = getParentId(currentId); parent != -1; parent = getParentId(parent)) {
                states[parent] = OutlineItemState.EXPANDED;
            }
            rebuild();
            if (getCount() == 1 && states[0] == OutlineItemState.COLLAPSED) {
                states[0] = OutlineItemState.EXPANDED;
                rebuild();
            }
        }
    }

    public int getParentId(final int id) {
        final int level = objects[id].level;
        for (int i = id - 1; i >= 0; i--) {
            if (objects[i].level < level) {
                return i;
            }
        }
        return -1;
    }

    protected void rebuild() {
        mapping.clear();
        int pos = 0;
        int level = Integer.MAX_VALUE;
        for (int cid = 0; cid < objects.length; cid++) {
            if (objects[cid].level <= level) {
                mapping.put(pos++, cid);
                if (states[cid] == OutlineItemState.COLLAPSED) {
                    level = objects[cid].level;
                } else {
                    level = Integer.MAX_VALUE;
                }
            }
        }
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public int getCount() {
        return mapping.size();
    }

    @Override
    public OutlineLink getItem(final int position) {
        final int id = mapping.get(position, -1);
        return id >= 0 && id < objects.length ? objects[id] : null;
    }

    @Override
    public long getItemId(final int position) {
        return mapping.get(position, -1);
    }

    public int getItemPosition(final OutlineLink item) {
        for (int i = 0, n = getCount(); i < n; i++) {
            if (item == getItem(i)) {
                return i;
            }
        }
        return -1;
    }

    public int getItemId(final OutlineLink item) {
        for (int i = 0, n = objects.length; i < n; i++) {
            if (item == objects[i]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final int id = (int) getItemId(position);
        View container = null;
        boolean firstTime = false;
        if (convertView == null) {
            container = LayoutInflater.from(context).inflate(R.layout.outline_item, parent, false);
            firstTime = true;
        } else {
            container = convertView;
        }
        final TextView view = (TextView) container.findViewById(R.id.treeview_item_title);
        final View btn = container.findViewById(R.id.treeview_collapse);
        final View go = container.findViewById(R.id.treeview_go);

        final OutlineLink item = getItem(position);
        view.setText(item.title.trim());
        view.setTag(position);
        btn.setTag(position);

        //container.setBackgroundDrawable(id == currentId ? this.selected : this.background);
        if (id == currentId)
        	container.setBackgroundDrawable(this.selected);
        else
        	container.setBackgroundColor(Color.WHITE);

        if (firstTime) {
            final LinearLayout.LayoutParams btnParams = (LayoutParams) btn.getLayoutParams();
            spaceWidth = btnParams.width;
            container.setOnClickListener(voidListener);
            view.setOnClickListener(itemListener);
        }

        final LinearLayout.LayoutParams sl = (LayoutParams) go.getLayoutParams();
        sl.leftMargin = Math.min(5, item.level) * spaceWidth;
        go.setLayoutParams(sl);

        if (states[id] == OutlineItemState.LEAF) {
        	go.setVisibility(View.VISIBLE);
            btn.setOnClickListener(voidListener);
            btn.setBackgroundColor(Color.TRANSPARENT);
        } else {
        	go.setVisibility(View.INVISIBLE);
            btn.setOnClickListener(collapseListener);
            btn.setBackgroundResource(states[id] == OutlineItemState.EXPANDED ? R.drawable.treeview_unexpand_popup
                    : R.drawable.treeview_expand_popup);
        }

        return container;
    }

    private static enum OutlineItemState {
        LEAF, EXPANDED, COLLAPSED;
    }

    private final class CollapseListener implements OnClickListener {

        @Override
        public void onClick(final View v) {
            System.out.println("btn.OnClickListener()");
            {
                final int position = ((Integer) v.getTag()).intValue();
                final int id = (int) getItemId(position);
                final OutlineItemState newState = states[id] == OutlineItemState.EXPANDED ? OutlineItemState.COLLAPSED
                        : OutlineItemState.EXPANDED;
                states[id] = newState;
            }
            rebuild();

            v.post(new Runnable() {

                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    private static final class ItemListener implements OnClickListener {

        @Override
        public void onClick(final View v) {
            for (ViewParent p = v.getParent(); p != null; p = p.getParent()) {
                if (p instanceof ListView) {
                    final ListView list = (ListView) p;
                    final OnItemClickListener l = list.getOnItemClickListener();
                    if (l != null) {
                        l.onItemClick(list, v, ((Integer) v.getTag()).intValue(), 0);
                    }
                    return;
                }
            }

        }
    }

    private static final class VoidListener implements OnClickListener {

        @Override
        public void onClick(final View v) {
        }
    }
}
