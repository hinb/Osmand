package net.osmand.plus.download.ui;

import java.text.DateFormat;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ItemViewHolder {


	protected final TextView nameTextView;
	protected final TextView descrTextView;
	protected final ImageView leftImageView;
	protected final ImageView rightImageButton;
	protected final Button rightButton;
	protected final ProgressBar progressBar;

	private boolean srtmDisabled;
	private boolean srtmNeedsInstallation;
	private boolean nauticalPluginDisabled;
	private boolean freeVersion;
	
	protected final DownloadActivity context;
	
	private int textColorPrimary;
	private int textColorSecondary;
	
	boolean showTypeInDesc;
	boolean showTypeInName;
	boolean showRemoteDate;
	boolean silentCancelDownload;
	boolean showProgressInDesc;
	
	private DateFormat dateFormat;

	

	private enum RightButtonAction {
		DOWNLOAD,
		ASK_FOR_SEAMARKS_PLUGIN,
		ASK_FOR_SRTM_PLUGIN_PURCHASE,
		ASK_FOR_SRTM_PLUGIN_ENABLE,
		ASK_FOR_FULL_VERSION_PURCHASE
	}
	

	public ItemViewHolder(View view, DownloadActivity context) {
		this.context = context;
		dateFormat = android.text.format.DateFormat.getMediumDateFormat(context);
		progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
		rightButton = (Button) view.findViewById(R.id.rightButton);
		leftImageView = (ImageView) view.findViewById(R.id.leftImageView);
		descrTextView = (TextView) view.findViewById(R.id.description);
		rightImageButton = (ImageView) view.findViewById(R.id.rightImageButton);
		nameTextView = (TextView) view.findViewById(R.id.name);
		

		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
		textColorPrimary = typedValue.data;
		theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
		textColorSecondary = typedValue.data;
	}
	
	public void setShowRemoteDate(boolean showRemoteDate) {
		this.showRemoteDate = showRemoteDate;
	}
	
	public void setShowProgressInDescr(boolean b) {
		showProgressInDesc = b;
	}
	
	public void setSilentCancelDownload(boolean silentCancelDownload) {
		this.silentCancelDownload = silentCancelDownload;
	}
	
	public void setShowTypeInDesc(boolean showTypeInDesc) {
		this.showTypeInDesc = showTypeInDesc;
	}
	
	public void setShowTypeInName(boolean showTypeInName) {
		this.showTypeInName = showTypeInName;
	}


	// FIXME don't initialize on every row 
	private void initAppStatusVariables() {
		srtmDisabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null;
		nauticalPluginDisabled = OsmandPlugin.getEnabledPlugin(NauticalMapsPlugin.class) == null;
		freeVersion = Version.isFreeVersion(context.getMyApplication());
		OsmandPlugin srtmPlugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		srtmNeedsInstallation = srtmPlugin == null || srtmPlugin.needsInstallation();
	}

	public void bindIndexItem(final IndexItem indexItem, final DownloadResourceGroup parentOptional) {
		initAppStatusVariables();
		boolean isDownloading = context.getDownloadThread().isDownloading(indexItem);
		int progress = -1;
		if (context.getDownloadThread().getCurrentDownloadingItem() == indexItem) {
			progress = context.getDownloadThread().getCurrentDownloadingItemProgress();
		}
		boolean disabled = checkDisabledAndClickAction(indexItem);
		/// name and left item
		if(showTypeInName) {
			nameTextView.setText(indexItem.getType().getString(context));
		} else {
			nameTextView.setText(indexItem.getVisibleName(context, context.getMyApplication().getRegions(), false));
		}
		if(!disabled) {
			nameTextView.setTextColor(textColorPrimary);
		} else {
			nameTextView.setTextColor(textColorSecondary);
		}
		int color = textColorSecondary;
		if(indexItem.isDownloaded() && !isDownloading) {
			int colorId = indexItem.isOutdated() ? R.color.color_distance : R.color.color_ok;
			color = context.getResources().getColor(colorId);
		}
		if (indexItem.isDownloaded()) {
			leftImageView.setImageDrawable(getContentIcon(context,
					indexItem.getType().getIconResource(), color));
		} else if (disabled) {
			leftImageView.setImageDrawable(getContentIcon(context,
					indexItem.getType().getIconResource(), textColorSecondary));
		} else {
			leftImageView.setImageDrawable(getContentIcon(context,
					indexItem.getType().getIconResource()));
		}
		descrTextView.setTextColor(textColorSecondary);
		if (!isDownloading) {
			progressBar.setVisibility(View.GONE);
			descrTextView.setVisibility(View.VISIBLE);
			if ((indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE ||
					indexItem.getType() == DownloadActivityType.HILLSHADE_FILE) && srtmDisabled) {
				if(showTypeInName) {
					descrTextView.setText("");
				} else {
					descrTextView.setText(indexItem.getType().getString(context));
				}
			} else if (showTypeInDesc) {
				descrTextView.setText(indexItem.getType().getString(context) + 
						" • " + indexItem.getSizeDescription(context) +
						" • " + (showRemoteDate ? indexItem.getRemoteDate(dateFormat) : indexItem.getLocalDate(dateFormat)));
			} else {
				descrTextView.setText(indexItem.getSizeDescription(context) + " • " + 
						(showRemoteDate ? indexItem.getRemoteDate(dateFormat) : indexItem.getLocalDate(dateFormat)));
			}
			
			rightImageButton.setImageDrawable(getContentIcon(context, R.drawable.ic_action_import));
			rightImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					download(indexItem, parentOptional);
				}
			});
		} else {
			progressBar.setVisibility(View.VISIBLE);
			progressBar.setIndeterminate(progress == -1);
			progressBar.setProgress(progress);
			
			if (showProgressInDesc) {
				double mb = indexItem.getArchiveSizeMB();
				String v ;
				if (progress != -1) {
					v = context.getString(R.string.value_downloaded_from_max, mb * progress / 100, mb);
				} else {
					v = context.getString(R.string.file_size_in_mb, mb);
				}
				if(showTypeInDesc && indexItem.getType() == DownloadActivityType.ROADS_FILE) {
					descrTextView.setText(indexItem.getType().getString(context) + " • " + v);
				} else {
					descrTextView.setText(v);
				}
				descrTextView.setVisibility(View.VISIBLE);
			} else {
				descrTextView.setVisibility(View.GONE);
			}
			
			rightImageButton.setImageDrawable(getContentIcon(context, R.drawable.ic_action_remove_dark));
			rightImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(silentCancelDownload) {
						context.getDownloadThread().cancelDownload(indexItem);
					} else {
						context.makeSureUserCancelDownload(indexItem);
					}
				}
			});
		}
	}


	protected void download(IndexItem indexItem, DownloadResourceGroup parentOptional) {
		boolean handled = false;
		if (indexItem.getType() == DownloadActivityType.ROADS_FILE && parentOptional != null) {
			for (IndexItem ii : parentOptional.getIndividualResources()) {
				if (ii.getType() == DownloadActivityType.NORMAL_FILE) {
					if (ii.isDownloaded()) {
						handled = true;
						confirmDownload(indexItem);
					}
					break;
				}
			}
		}		
		if(!handled) {
			context.startDownload(indexItem);
		}
	}
	private void confirmDownload(final IndexItem indexItem) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.are_you_sure);
		builder.setMessage(R.string.confirm_download_roadmaps);
		builder.setNegativeButton(R.string.shared_string_cancel, null).setPositiveButton(
				R.string.shared_string_download, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (indexItem != null) {
							context.startDownload(indexItem);
						}
					}
				});
		builder.show();
	}

	private boolean checkDisabledAndClickAction(final IndexItem indexItem) {
		RightButtonAction clickAction = getClickAction(indexItem);
		boolean disabled = clickAction != RightButtonAction.DOWNLOAD;
		if (clickAction != RightButtonAction.DOWNLOAD) {
			rightButton.setText(R.string.get_plugin);
			rightButton.setVisibility(View.VISIBLE);
			rightImageButton.setVisibility(View.GONE);
			rightButton.setOnClickListener(getRightButtonAction(indexItem, clickAction, null));
		} else {
			rightButton.setVisibility(View.GONE);
			rightImageButton.setVisibility(View.VISIBLE);
		}
		
		return disabled;
	}

	@SuppressLint("DefaultLocale")
	public RightButtonAction getClickAction(final IndexItem indexItem) {
		RightButtonAction clickAction = RightButtonAction.DOWNLOAD;
		if (indexItem.getBasename().toLowerCase().equals(DownloadResources.WORLD_SEAMARKS_KEY)
				&& nauticalPluginDisabled) {
			clickAction = RightButtonAction.ASK_FOR_SEAMARKS_PLUGIN;
		} else if ((indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE ||
				indexItem.getType() == DownloadActivityType.HILLSHADE_FILE) && srtmDisabled) {
			if (srtmNeedsInstallation) {
				clickAction = RightButtonAction.ASK_FOR_SRTM_PLUGIN_PURCHASE;
			} else {
				clickAction = RightButtonAction.ASK_FOR_SRTM_PLUGIN_ENABLE;
			}

		} else if (indexItem.getType() == DownloadActivityType.WIKIPEDIA_FILE && freeVersion) {
			clickAction = RightButtonAction.ASK_FOR_FULL_VERSION_PURCHASE;
		}
		return clickAction;
	}

	public OnClickListener getRightButtonAction(final IndexItem item, final RightButtonAction clickAction, final DownloadResourceGroup parentOptional) {
		if (clickAction != RightButtonAction.DOWNLOAD) {
			return new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (clickAction) {
					case ASK_FOR_FULL_VERSION_PURCHASE:
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.marketPrefix(context
								.getMyApplication()) + "net.osmand.plus"));
						context.startActivity(intent);
						break;
					case ASK_FOR_SEAMARKS_PLUGIN:
						context.startActivity(new Intent(context, context.getMyApplication().getAppCustomization()
								.getPluginsActivity()));
						AccessibleToast.makeText(context.getApplicationContext(),
								context.getString(R.string.activate_seamarks_plugin), Toast.LENGTH_SHORT).show();
						break;
					case ASK_FOR_SRTM_PLUGIN_PURCHASE:
						OsmandPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
						context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL())));
						break;
					case ASK_FOR_SRTM_PLUGIN_ENABLE:
						context.startActivity(new Intent(context, context.getMyApplication().getAppCustomization()
								.getPluginsActivity()));
						AccessibleToast.makeText(context, context.getString(R.string.activate_srtm_plugin),
								Toast.LENGTH_SHORT).show();
						break;
					case DOWNLOAD:
						break;
					}
				}
			};
		} else {
			final boolean isDownloading = context.getDownloadThread().isDownloading(item);
			return new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(isDownloading) {
						if(silentCancelDownload) {
							context.getDownloadThread().cancelDownload(item);
						} else {
							context.makeSureUserCancelDownload(item);
						}
					} else {
						download(item, parentOptional);
					}
				}
			};
		}
	}

	private Drawable getContentIcon(DownloadActivity context, int resourceId) {
		return context.getMyApplication().getIconsCache().getContentIcon(resourceId);
	}

	private Drawable getContentIcon(DownloadActivity context, int resourceId, int color) {
		return context.getMyApplication().getIconsCache().getPaintedContentIcon(resourceId, color);
	}

	
}
