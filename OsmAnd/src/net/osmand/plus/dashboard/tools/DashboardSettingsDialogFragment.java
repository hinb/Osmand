package net.osmand.plus.dashboard.tools;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashboardOnMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardSettingsDialogFragment extends DialogFragment
		implements NumberPickerDialogFragment.CanAcceptNumber {
	private static final org.apache.commons.logging.Log LOG =
			PlatformUtil.getLog(NumberPickerDialogFragment.class);
	private static final String CHECKED_ITEMS = "checked_items";
	private static final String NUMBER_OF_ROWS_ARRAY = "number_of_rows_array";
	private MapActivity mapActivity;
	private ArrayList<DashFragmentData> mFragmentsData;
	private DashFragmentAdapter mAdapter;
	private int textColorPrimary;
	private int textColorSecondary;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mapActivity = (MapActivity) activity;
		mFragmentsData = new ArrayList<>();
		for (DashFragmentData fragmentData : mapActivity.getDashboard().getFragmentsData()) {
			if (fragmentData.canBeDisabled()) mFragmentsData.add(fragmentData);
		}
		mFragmentsData.addAll(OsmandPlugin.getPluginsCardsList());
		Collections.sort(mFragmentsData);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = getActivity().getTheme();
		theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
		textColorPrimary = typedValue.data;
		theme.resolveAttribute(R.attr.dialog_inactive_text_color, typedValue, true);
		textColorSecondary = typedValue.data;

		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();

		View view = LayoutInflater.from(getActivity()).inflate(
				R.layout.show_dashboard_on_start_dialog_item, null, false);
		final TextView textView = (TextView) view.findViewById(R.id.text);
		textView.setText(R.string.show_on_start);
		final OsmandSettings.CommonPreference<Boolean> shouldShowDashboardOnStart =
				settings.registerBooleanPreference(MapActivity.SHOULD_SHOW_DASHBOARD_ON_START, true);
		final CompoundButton compoundButton = (CompoundButton) view.findViewById(R.id.check_item);
		compoundButton.setChecked(shouldShowDashboardOnStart.get());
		textView.setTextColor(shouldShowDashboardOnStart.get() ? textColorPrimary
				: textColorSecondary);
		compoundButton.setOnCheckedChangeListener(
				new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
						textView.setTextColor(b ? textColorPrimary : textColorSecondary);
					}
				});

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		if (savedInstanceState != null && savedInstanceState.containsKey(CHECKED_ITEMS)) {
			mAdapter = new DashFragmentAdapter(getActivity(), mFragmentsData,
					savedInstanceState.getBooleanArray(CHECKED_ITEMS),
					savedInstanceState.getIntArray(NUMBER_OF_ROWS_ARRAY));
		} else {
			mAdapter = new DashFragmentAdapter(getActivity(), mFragmentsData, settings);
		}
		builder.setTitle(R.string.dahboard_options_dialog_title)
				.setAdapter(mAdapter, null)
				.setPositiveButton(R.string.shared_string_apply, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int type) {
						boolean[] shouldShow = mAdapter.getCheckedItems();
						int[] numberOfRows = mAdapter.getNumbersOfRows();
						for (int i = 0; i < shouldShow.length; i++) {
							final DashFragmentData fragmentData = mFragmentsData.get(i);
							settings.registerBooleanPreference(
									DashboardOnMap.SHOULD_SHOW + fragmentData.tag, true)
									.makeGlobal().set(shouldShow[i]);
							if (fragmentData.rowNumberTag != null) {
								settings.registerIntPreference(fragmentData.rowNumberTag, 5)
										.makeGlobal().set(numberOfRows[i]);
							}
						}
						mapActivity.getDashboard().refreshDashboardFragments();
						shouldShowDashboardOnStart.set(compoundButton.isChecked());
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null);
		final AlertDialog dialog = builder.create();

		ListView listView = dialog.getListView();
		listView.addHeaderView(view);
		return dialog;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBooleanArray(CHECKED_ITEMS, mAdapter.getCheckedItems());
		outState.putIntArray(NUMBER_OF_ROWS_ARRAY, mAdapter.getNumbersOfRows());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void acceptNumber(String tag, int number) {
		mAdapter.getNumbersOfRows()[Integer.parseInt(tag)] = number;
		mAdapter.notifyDataSetChanged();
	}

	private class DashFragmentAdapter extends ArrayAdapter<DashFragmentData> {
		private final boolean[] checkedItems;
		private final int[] numbersOfRows;
		private final int colorBlue;

		public DashFragmentAdapter(@NonNull Context context,
								   @NonNull List<DashFragmentData> objects,
								   @NonNull boolean[] checkedItems,
								   @NonNull int[] numbersOfRows) {
			super(context, 0, objects);
			this.checkedItems = checkedItems;
			this.numbersOfRows = numbersOfRows;
			colorBlue = getContext().getResources().getColor(R.color.dashboard_blue);
		}

		public DashFragmentAdapter(@NonNull Context context,
								   @NonNull List<DashFragmentData> objects,
								   @NonNull OsmandSettings settings) {
			super(context, 0, objects);
			numbersOfRows = new int[objects.size()];
			checkedItems = new boolean[objects.size()];
			for (int i = 0; i < objects.size(); i++) {
				checkedItems[i] = settings.registerBooleanPreference(
						DashboardOnMap.SHOULD_SHOW + objects.get(i).tag, true).makeGlobal().get();
				if (objects.get(i).tag != null) {
					numbersOfRows[i] = settings.registerIntPreference(objects.get(i).rowNumberTag, 5)
							.makeGlobal().get();
				}
			}
			colorBlue = getContext().getResources().getColor(R.color.dashboard_blue);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final DashViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(
						R.layout.dashboard_settings_dialog_item, parent, false);
				viewHolder = new DashViewHolder(convertView);
			} else {
				viewHolder = (DashViewHolder) convertView.getTag();
			}
			viewHolder.bindDashView(getItem(position), position);
			convertView.setTag(viewHolder);
			return convertView;
		}

		public boolean[] getCheckedItems() {
			return checkedItems;
		}

		public int[] getNumbersOfRows() {
			return numbersOfRows;
		}

		private class DashViewHolder {
			final TextView textView;
			final CompoundButton compoundButton;
			final TextView numberOfRowsTextView;
			private int position;

			public DashViewHolder(View view) {
				this.numberOfRowsTextView = (TextView) view.findViewById(R.id.numberOfRowsTextView);
				this.textView = (TextView) view.findViewById(R.id.text);
				this.compoundButton = (CompoundButton) view.findViewById(R.id.check_item);
			}

			public void bindDashView(DashFragmentData fragmentData, int position) {
				if (fragmentData.hasRows()) {
					numberOfRowsTextView.setVisibility(View.VISIBLE);
					numberOfRowsTextView.setText(String.valueOf(numbersOfRows[position]));
					numberOfRowsTextView.setTextColor(checkedItems[position] ? colorBlue :
							textColorSecondary);
				} else {
					numberOfRowsTextView.setVisibility(View.GONE);
				}
				textView.setText(fragmentData.titleStringId);
				textView.setTextColor(checkedItems[position] ? textColorPrimary :
						textColorSecondary);
				this.position = position;

				compoundButton.setChecked(checkedItems[position]);
				compoundButton.setTag(this);
				compoundButton.setOnCheckedChangeListener(onTurnedOnOffListener);

				numberOfRowsTextView.setTag(this);
				numberOfRowsTextView.setOnClickListener(onNumberClickListener);
			}
		}

		final CompoundButton.OnCheckedChangeListener onTurnedOnOffListener =
				new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
						DashViewHolder localViewHolder = (DashViewHolder) compoundButton.getTag();
						if (localViewHolder == null) return;
						checkedItems[localViewHolder.position] = b;
						localViewHolder.textView.setTextColor(
								checkedItems[localViewHolder.position] ? textColorPrimary
										: textColorSecondary);
						localViewHolder.numberOfRowsTextView.setTextColor(
								checkedItems[localViewHolder.position] ? colorBlue
										: textColorSecondary);
					}
				};

		final View.OnClickListener onNumberClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DashViewHolder localViewHolder = (DashViewHolder) v.getTag();
				String header = getContext().getString(getItem(localViewHolder.position).titleStringId);
				String subheader = getContext().getResources().getString(R.string.count_of_lines);
				final String stringPosition = String.valueOf(localViewHolder.position);
				NumberPickerDialogFragment
						.createInstance(header, subheader, stringPosition, 5)
						.show(getChildFragmentManager(), NumberPickerDialogFragment.TAG);
			}
		};
	}
}
