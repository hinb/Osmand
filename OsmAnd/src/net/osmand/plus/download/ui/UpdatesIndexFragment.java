package net.osmand.plus.download.ui;

import java.util.Comparator;
import java.util.List;

import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmAndListFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class UpdatesIndexFragment extends OsmAndListFragment implements DownloadEvents {
	private static final int RELOAD_ID = 5;
	private UpdateIndexAdapter listAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		invalidateListView();
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.update_index_frament, container, false);
	}
	
	@Override
	public ArrayAdapter<?> getAdapter() {
		return listAdapter;
	}
	
	@Override
	public void downloadHasFinished() {
		invalidateListView();
		updateUpdateAllButton();
	}
	
	@Override
	public void downloadInProgress() {
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void newDownloadIndexes() {
		invalidateListView();
		updateUpdateAllButton();
	}

	public void invalidateListView() {
		DownloadResources indexes = getMyActivity().getDownloadThread().getIndexes();
		List<IndexItem> indexItems = indexes.getItemsToUpdate();
		if (indexItems.size() == 0) {
			if (indexes.isDownloadedFromInternet) {
				indexItems.add(new IndexItem(getString(R.string.everything_up_to_date), "", 0, "", 0, 0, null));
			} else {
				indexItems.add(new IndexItem(getString(R.string.no_index_file_to_download), "", 0, "", 0, 0, null));
			}
		}
		final OsmandRegions osmandRegions =
				getMyApplication().getResourceManager().getOsmandRegions();
		listAdapter = new UpdateIndexAdapter(getMyActivity(), R.layout.download_index_list_item, indexItems);
		listAdapter.sort(new Comparator<IndexItem>() {
			@Override
			public int compare(IndexItem indexItem, IndexItem indexItem2) {
				return indexItem.getVisibleName(getMyApplication(), osmandRegions)
						.compareTo(indexItem2.getVisibleName(getMyApplication(), osmandRegions));
			}
		});
		setListAdapter(listAdapter);
	}

	private void updateUpdateAllButton() {
		
		View view = getView();
		if (view == null) {
			return;
		}
		DownloadResources indexes = getMyActivity().getDownloadThread().getIndexes();
		final List<IndexItem> indexItems = indexes.getItemsToUpdate();
		final TextView updateAllButton = (TextView) view.findViewById(R.id.updateAllButton);
		if (indexItems.size() == 0 || indexItems.get(0).getType() == null) {
			updateAllButton.setVisibility(View.GONE);
		} else {
			updateAllButton.setVisibility(View.VISIBLE);
			long downloadsSize = 0;
			for (IndexItem indexItem : indexItems) {
				downloadsSize += indexItem.getSize();
			}
			String updateAllText = getActivity().getString(R.string.update_all, downloadsSize >> 20);
			updateAllButton.setText(updateAllText);
			updateAllButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getMyActivity().startDownload(indexItems.toArray(new IndexItem[indexItems.size()]));
				}
			});
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updateUpdateAllButton();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final IndexItem e = (IndexItem) getListAdapter().getItem(position);
		ItemViewHolder vh = (ItemViewHolder) v.getTag();
		OnClickListener ls = vh.getRightButtonAction(e, vh.getClickAction(e), null);
		ls.onClick(v);
	}

	public DownloadActivity getMyActivity() {
		return (DownloadActivity) getActivity();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		ActionBar actionBar = getMyActivity().getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

		if (getMyApplication().getAppCustomization().showDownloadExtraActions()) {
			MenuItem item = menu.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
			item.setIcon(R.drawable.ic_action_refresh_dark);
			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
	}

	public OsmandApplication getMyApplication() {
		return getMyActivity().getMyApplication();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == RELOAD_ID) {
			// re-create the thread
			getMyActivity().getDownloadThread().runReloadIndexFiles();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class UpdateIndexAdapter extends ArrayAdapter<IndexItem> {
		List<IndexItem> items;

		public UpdateIndexAdapter(Context context, int resource, List<IndexItem> items) {
			super(context, resource, items);
			this.items = items;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = LayoutInflater.from(getMyActivity());
				v = inflater.inflate(R.layout.two_line_with_images_list_item, parent, false);
				v.setTag(new ItemViewHolder(v, getMyActivity()));
				
			}
			ItemViewHolder holder = (ItemViewHolder) v.getTag();
			holder.setShowRemoteDate(true);
			holder.setShowTypeInDesc(true);
			holder.bindIndexItem(items.get(position), null);
			return v;
		}
	}

}
