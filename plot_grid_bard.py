import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import numpy as np

# Create sample data
data = np.random.randint(0, 10, (5, 5))

# Create the figure and heatmap
fig, ax = plt.subplots()
im = ax.imshow(data, cmap='viridis')

# Get unique values, counts, and colors
values, counts = np.unique(data.ravel(), return_counts=True)
colors = [im.cmap(im.norm(value)) for value in values]

# Create patches and labels for the legend
patches = [mpatches.Patch(color=colors[i], label=f"{values[i]} ({counts[i]})") for i in range(len(values))]

# Add the legend
ax.legend(handles=patches, loc='upper left', title='Count Distribution')

# Add a colorbar
plt.colorbar(im, label='Data Values', ax=ax)

# Show the plot
plt.tight_layout()
plt.show()
