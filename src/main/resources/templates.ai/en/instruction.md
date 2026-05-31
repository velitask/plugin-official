# User Instructions: How to Create Similar Velitask Templates

This guide helps the AI assistant adapt a HUD template to your actual working `.vttp` files instead of inventing a template from scratch.

## What to Prepare

Prepare 2 files:

1. Your working `.vttp` video template.
   - Ideally, first create a simple template manually in Velitask.
   - For example: video + one or two indicators.
   - The most important thing is that the file must open correctly in Velitask.

2. Your working `.vttp` map template.
   - For example, a template that already contains the `geoMap` layer.
   - It must also open and work correctly in Velitask.

## Workflow

### Step 1. Send Your Working Video Template

First, upload your working `.vttp` file for video.

Write:

```text
Here is my working Velitask video template. Study its structure and make all further changes only based on this file. Do not rebuild the structure from scratch.
```

Wait until the AI assistant inspects the file.

### Step 2. Send the Desired Template Example

Next, send an example of the style you want to reproduce.

This can be:
- a finished `.vttp` template you like;
- an image showing the desired HUD style;
- a written description of the layout.

Write:

```text
Now adapt my template to this style. Make changes in small steps and name files sequentially: filename_001.vttp, filename_002.vttp, and so on.
```

### Step 3. Paste the Main Prompt

After uploading the files, paste the main prompt from the publication.

Important: do not paste the prompt before uploading the files, because the AI assistant needs to see your real `.vttp` structure first.

### Step 4. Test Every File in Velitask

After each generated file:
1. download the `.vttp`;
2. apply it in Velitask;
3. check whether the video/map is still visible;
4. check whether the indicators appear correctly;
5. if something breaks, say which numbered file broke.

Useful continuation phrase:

```text
This file works. Continue.
```

If the file breaks:

```text
This file does not work. Go back to the previous working version and add only one next element.
```

## How to Ask for Changes

Ask for small changes step by step.

Bad request:

```text
Make everything beautiful at once: add all indicators, map, speedometer, graphs, and colors.
```

Good request:

```text
Add only the right wrapper.
```

Then:

```text
Now add only the icon.
```

Then:

```text
Now add only the title.
```

Then:

```text
Now add only the distance value.
```

This makes it much easier to understand which exact layer or parameter breaks the template.

## Important Rules

- Do not ask the AI assistant to fully rewrite the `.vttp` if you already have a working file.
- Always ask it to use the latest working template as the base.
- If a new indicator is added, first add it in its minimal working form.
- After testing, you can improve styling.
- For the map template, first keep the `geoMap` layer unchanged and simply add the HUD on top.
- For the video template, keep the `video` layer unchanged and add the HUD on top.
- If fonts become too large on the map, ask the AI assistant to scale them proportionally to the canvas size.
- For adaptive placement, ask it to use `maket.preset: "proportional"`.

## Useful Links

Velitask indicator documentation:
https://github.com/velitask/plugin-official/wiki

Mixel layout documentation:
https://velitask.com/docs_ru.html#concepts-mixel-layouts
